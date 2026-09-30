package com.pulse.bluetoothdisable.cover.calendar

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.time.LocalDate
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

class CalendarAccessManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME, Context.MODE_PRIVATE,
    )

    fun setAccessRule(date: LocalDate, text: String) {
        require(CalendarAccessPolicy.isValid(text))
        require(date.year in CalendarDates.YEAR_RANGE)
        val signature = sign(existingKey() ?: generateKey(), date, text)
        check(preferences.edit()
            .putString(KEY_DATE, date.toString())
            .putString(KEY_MAC, Base64.encodeToString(signature, Base64.NO_WRAP))
            .commit()) { "Unable to persist calendar access rule" }
    }

    fun verify(date: LocalDate, text: String): Boolean {
        if (!CalendarAccessPolicy.isValid(text) || secretDate() != date) return false
        return try {
            val encoded = preferences.getString(KEY_MAC, null) ?: return false
            val stored = Base64.decode(encoded, Base64.NO_WRAP)
            val key = existingKey() ?: return false
            MessageDigest.isEqual(stored, sign(key, date, text))
        } catch (_: Exception) {
            false
        }
    }

    fun secretDate(): LocalDate? = try {
        preferences.getString(KEY_DATE, null)?.let(LocalDate::parse)
    } catch (_: Exception) {
        null
    }

    fun hasRule(): Boolean = try {
        val date = secretDate()
        val encoded = preferences.getString(KEY_MAC, null)
        date != null && date.year in CalendarDates.YEAR_RANGE && encoded != null &&
            Base64.decode(encoded, Base64.NO_WRAP).size == 32 && existingKey() != null
    } catch (_: Exception) {
        false
    }

    /** Retain the device key until the journal commits so recovery can roll back. */
    internal fun clearVerifier() {
        check(preferences.edit().clear().commit()) { "Unable to clear calendar access rule" }
    }

    fun clear() {
        clearVerifier()
        deleteKey()
    }

    internal fun deleteKey() {
        try {
            keyStore().deleteEntry(KEY_ALIAS)
        } catch (_: Exception) {
            // A leftover device key cannot verify anything without the stored verifier.
        }
    }

    private fun sign(key: SecretKey, date: LocalDate, text: String): ByteArray =
        Mac.getInstance("HmacSHA256").run {
            init(key)
            doFinal(CalendarAccessPolicy.payload(date, text))
        }

    private fun existingKey(): SecretKey? = keyStore().getKey(KEY_ALIAS, null) as? SecretKey

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun generateKey(): SecretKey =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(
                KEY_ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
            ).setDigests(KeyProperties.DIGEST_SHA256).build())
            generateKey()
        }

    companion object {
        const val PREFERENCES_NAME = "calendar_access_preferences"
        private const val KEY_DATE = "secret_date"
        private const val KEY_MAC = "secret_text_hmac"
        private const val KEY_ALIAS = "bluetooth_disabler_calendar_access_v1"
    }
}
