package com.pulse.bluetoothdisable.cover.calendar

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypt every note equally so an access note is not plaintext in app files either. */
internal object CalendarNotesCipher {
    private const val KEY_ALIAS = "bluetooth_disabler_calendar_notes_v1"
    private val aad = "calendar-notes-v1".toByteArray(Charsets.UTF_8)

    fun encrypt(json: String): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, existingKey() ?: generateKey())
        cipher.updateAAD(aad)
        return JSONObject().apply {
            put("version", 1)
            put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            put("data", Base64.encodeToString(cipher.doFinal(json.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP))
        }.toString().toByteArray(Charsets.UTF_8)
    }

    fun decrypt(envelope: String): String {
        val data = JSONObject(envelope)
        require(data.getInt("version") == 1)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = checkNotNull(existingKey()) { "Missing calendar notes key" }
        cipher.init(Cipher.DECRYPT_MODE, key,
            GCMParameterSpec(128, Base64.decode(data.getString("iv"), Base64.NO_WRAP)))
        cipher.updateAAD(aad)
        return String(cipher.doFinal(Base64.decode(data.getString("data"), Base64.NO_WRAP)), Charsets.UTF_8)
    }

    fun clearKey() {
        try { keyStore().deleteEntry(KEY_ALIAS) } catch (_: Exception) {
            // The encrypted file has already been removed.
        }
    }

    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private fun existingKey(): SecretKey? = keyStore().getKey(KEY_ALIAS, null) as? SecretKey
    private fun generateKey(): SecretKey = KeyGenerator.getInstance("AES", "AndroidKeyStore").run {
        init(KeyGenParameterSpec.Builder(KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build())
        generateKey()
    }
}
