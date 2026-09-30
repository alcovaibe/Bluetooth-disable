package com.pulse.bluetoothdisable.cover.gallery

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Local-only AES-256-GCM storage independent from the temporary Gallery access rule. */
internal object GalleryCipher {
    private const val KEY_ALIAS = "bluetooth_disabler_gallery_data_v1"
    private const val VERSION: Byte = 1

    fun encrypt(bytes: ByteArray, purpose: String): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, existingKey() ?: generateKey())
        cipher.updateAAD(aad(purpose))
        val encrypted = cipher.doFinal(bytes)
        val iv = cipher.iv
        return ByteBuffer.allocate(2 + iv.size + encrypted.size)
            .put(VERSION)
            .put(iv.size.toByte())
            .put(iv)
            .put(encrypted)
            .array()
    }

    fun decrypt(envelope: ByteArray, purpose: String): ByteArray {
        require(envelope.size > 14) { "Invalid gallery envelope" }
        val buffer = ByteBuffer.wrap(envelope)
        require(buffer.get() == VERSION) { "Unsupported gallery envelope" }
        val ivLength = buffer.get().toInt() and 0xff
        require(ivLength in 12..16 && buffer.remaining() > ivLength) { "Invalid gallery IV" }
        val iv = ByteArray(ivLength).also(buffer::get)
        val encrypted = ByteArray(buffer.remaining()).also(buffer::get)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = checkNotNull(existingKey()) { "Missing gallery storage key" }
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(aad(purpose))
        return cipher.doFinal(encrypted)
    }

    internal fun clearKey() {
        try {
            keyStore().deleteEntry(KEY_ALIAS)
        } catch (_: Exception) {
            // Data removal may race with an already absent key.
        }
    }

    private fun aad(purpose: String) = "gallery-data-v1\u0000$purpose".toByteArray(Charsets.UTF_8)

    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private fun existingKey(): SecretKey? = keyStore().getKey(KEY_ALIAS, null) as? SecretKey

    private fun generateKey(): SecretKey =
        KeyGenerator.getInstance("AES", "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
}
