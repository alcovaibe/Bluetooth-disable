package com.pulse.bluetoothdisable.cover.calculator

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

class CalculatorAccessCodeManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun setCode(code: String) {
        require(CalculatorAccessCodePolicy.isValid(code))
        val signature = sign(getOrCreateKey(), code)
        val encoded = Base64.encodeToString(signature, Base64.NO_WRAP)
        check(preferences.edit().putString(KEY_CODE_MAC, encoded).commit()) {
            "Unable to persist calculator access verifier"
        }
    }

    fun verify(code: String): Boolean {
        if (!CalculatorAccessCodePolicy.isValid(code)) return false
        val stored = preferences.getString(KEY_CODE_MAC, null) ?: return false
        val storedBytes = try {
            Base64.decode(stored, Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            return false
        }

        return try {
            val key = existingKey() ?: return false
            MessageDigest.isEqual(storedBytes, sign(key, code))
        } catch (_: Exception) {
            false
        }
    }

    fun hasCode(): Boolean {
        if (!preferences.contains(KEY_CODE_MAC)) return false
        return try {
            existingKey() != null
        } catch (_: Exception) {
            false
        }
    }

    fun clearCode() {
        preferences.edit().clear().commit()
        try {
            val keyStore = loadKeyStore()
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        } catch (_: Exception) {
            // Preferences are already cleared. Never expose key/verification details.
        }
    }

    private fun sign(key: SecretKey, code: String): ByteArray =
        Mac.getInstance(HMAC_ALGORITHM).run {
            init(key)
            doFinal(code.toByteArray(Charsets.UTF_8))
        }

    private fun getOrCreateKey(): SecretKey = existingKey() ?: generateKey()

    private fun existingKey(): SecretKey? =
        loadKeyStore().getKey(KEY_ALIAS, null) as? SecretKey

    private fun loadKeyStore(): KeyStore =
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    private fun generateKey(): SecretKey {
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
            KEYSTORE_PROVIDER,
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        const val PREFERENCES_NAME = "calculator_access_code_preferences"
        private const val KEY_CODE_MAC = "access_code_hmac"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "bluetooth_disabler_calculator_access_v1"
        private const val HMAC_ALGORITHM = "HmacSHA256"
    }
}
