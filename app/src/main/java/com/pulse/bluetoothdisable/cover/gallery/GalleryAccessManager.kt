package com.pulse.bluetoothdisable.cover.gallery

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

class GalleryAccessManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun setAccessRule(imageId: String, sequence: List<GalleryTapZone>) {
        require(imageId.isNotBlank())
        require(GalleryAccessPolicy.isValidSequence(sequence))
        val key = existingKey() ?: generateKey()
        val signature = sign(key, imageId, sequence)
        check(
            preferences.edit()
                .putString(KEY_IMAGE_ID, imageId)
                .putString(KEY_MAC, Base64.encodeToString(signature, Base64.NO_WRAP))
                .commit(),
        ) { "Unable to persist gallery access rule" }
    }

    fun secretImageId(): String? =
        preferences.getString(KEY_IMAGE_ID, null)?.takeIf { it.isNotBlank() }

    fun hasRule(): Boolean {
        return try {
            val imageId = secretImageId() ?: return false
            val encoded = preferences.getString(KEY_MAC, null) ?: return false
            imageId.isNotBlank() && Base64.decode(encoded, Base64.NO_WRAP).size == 32 && existingKey() != null
        } catch (_: Exception) {
            false
        }
    }

    fun isSecretImage(imageId: String): Boolean = secretImageId() == imageId

    fun matches(imageId: String, sequence: List<GalleryTapZone>): Boolean {
        if (secretImageId() != imageId || !GalleryAccessPolicy.isValidSequence(sequence)) return false
        return try {
            val stored = Base64.decode(
                preferences.getString(KEY_MAC, null) ?: return false,
                Base64.NO_WRAP,
            )
            val key = existingKey() ?: return false
            MessageDigest.isEqual(stored, sign(key, imageId, sequence))
        } catch (_: Exception) {
            false
        }
    }

    /** Retain the key until the cover transition commits so rollback can restore the verifier. */
    internal fun clearVerifier() {
        check(preferences.edit().clear().commit()) { "Unable to clear gallery access rule" }
    }

    fun clear() {
        clearVerifier()
        deleteKey()
    }

    internal fun deleteKey() {
        try {
            keyStore().deleteEntry(KEY_ALIAS)
        } catch (_: Exception) {
            // A key without the stored verifier cannot authorize anything.
        }
    }

    private fun sign(
        key: SecretKey,
        imageId: String,
        sequence: List<GalleryTapZone>,
    ): ByteArray = Mac.getInstance("HmacSHA256").run {
        init(key)
        val encoded = sequence.joinToString("|") { it.name }
        doFinal("$imageId\u0000$encoded".toByteArray(Charsets.UTF_8))
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private fun existingKey(): SecretKey? = keyStore().getKey(KEY_ALIAS, null) as? SecretKey

    private fun generateKey(): SecretKey =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
                ).setDigests(KeyProperties.DIGEST_SHA256).build(),
            )
            generateKey()
        }

    companion object {
        const val PREFERENCES_NAME = "gallery_access_preferences"
        private const val KEY_IMAGE_ID = "secret_image_id"
        private const val KEY_MAC = "secret_sequence_hmac"
        private const val KEY_ALIAS = "bluetooth_disabler_gallery_access_v1"
    }
}
