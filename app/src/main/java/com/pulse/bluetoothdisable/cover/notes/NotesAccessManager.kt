package com.pulse.bluetoothdisable.cover.notes

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

data class NotesAccessRule(val noteId: String, val start: Int, val end: Int)

class NotesAccessManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun setAccessRule(noteId: String, body: String, start: Int, end: Int) {
        require(noteId.isNotBlank())
        require(NotesPolicy.isValidSecretRange(body, start, end))
        val signature = sign(existingKey() ?: generateKey(), noteId, start, end, body.substring(start, end))
        check(
            preferences.edit()
                .putString(KEY_NOTE_ID, noteId)
                .putString(KEY_START, start.toString())
                .putString(KEY_END, end.toString())
                .putString(KEY_MAC, Base64.encodeToString(signature, Base64.NO_WRAP))
                .commit(),
        ) { "Unable to persist notes access rule" }
    }

    fun rule(): NotesAccessRule? {
        return try {
            val noteId = preferences.getString(KEY_NOTE_ID, null)?.takeIf { it.isNotBlank() } ?: return null
            val start = preferences.getString(KEY_START, null)?.toIntOrNull() ?: return null
            val end = preferences.getString(KEY_END, null)?.toIntOrNull() ?: return null
            if (start < 0 || end <= start || end - start > NotesPolicy.MAX_SECRET_LENGTH) return null
            NotesAccessRule(noteId, start, end)
        } catch (_: Exception) {
            null
        }
    }

    fun hasRule(): Boolean {
        return try {
            val rule = rule() ?: return false
            val encoded = preferences.getString(KEY_MAC, null) ?: return false
            rule.noteId.isNotBlank() && Base64.decode(encoded, Base64.NO_WRAP).size == 32 && existingKey() != null
        } catch (_: Exception) {
            false
        }
    }

    fun isSecretNote(noteId: String): Boolean = rule()?.noteId == noteId

    fun matchesTap(noteId: String, body: String, offset: Int): Boolean {
        val rule = rule() ?: return false
        if (rule.noteId != noteId || offset !in rule.start until rule.end) return false
        return verifyCurrentBody(noteId, body, rule)
    }

    /** True for non-secret notes; for the secret note verifies that the configured range still matches. */
    fun noteStillMatches(noteId: String, body: String): Boolean {
        val rule = rule() ?: return true
        if (rule.noteId != noteId) return true
        return verifyCurrentBody(noteId, body, rule)
    }

    private fun verifyCurrentBody(noteId: String, body: String, rule: NotesAccessRule): Boolean {
        if (!NotesPolicy.isValidSecretRange(body, rule.start, rule.end)) return false
        return try {
            val encoded = preferences.getString(KEY_MAC, null) ?: return false
            val stored = Base64.decode(encoded, Base64.NO_WRAP)
            val key = existingKey() ?: return false
            val actual = sign(key, noteId, rule.start, rule.end, body.substring(rule.start, rule.end))
            MessageDigest.isEqual(stored, actual)
        } catch (_: Exception) {
            false
        }
    }

    /** Retain the key until a cover transition commits so rollback can restore the verifier. */
    internal fun clearVerifier() {
        check(preferences.edit().clear().commit()) { "Unable to clear notes access rule" }
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
        noteId: String,
        start: Int,
        end: Int,
        fragment: String,
    ): ByteArray = Mac.getInstance("HmacSHA256").run {
        init(key)
        doFinal("$noteId\u0000$start\u0000$end\u0000$fragment".toByteArray(Charsets.UTF_8))
    }

    private fun existingKey(): SecretKey? = keyStore().getKey(KEY_ALIAS, null) as? SecretKey

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

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
        const val PREFERENCES_NAME = "notes_access_preferences"
        private const val KEY_NOTE_ID = "secret_note_id"
        private const val KEY_START = "secret_start"
        private const val KEY_END = "secret_end"
        private const val KEY_MAC = "secret_fragment_hmac"
        private const val KEY_ALIAS = "bluetooth_disabler_notes_access_v1"
    }
}
