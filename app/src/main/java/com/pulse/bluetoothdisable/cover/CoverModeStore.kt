package com.pulse.bluetoothdisable.cover

import android.content.Context
import org.json.JSONObject

class CoverModeStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun activeMode(): CoverMode {
        val stored = preferences.getString(KEY_ACTIVE_MODE, null)
        return CoverMode.entries.firstOrNull { it.name == stored } ?: CoverMode.DEFAULT
    }

    fun setActiveMode(mode: CoverMode) {
        check(
            preferences.edit()
                .putString(KEY_ACTIVE_MODE, mode.name)
                .commit(),
        ) { "Unable to persist cover mode" }
    }

    fun markPending(mode: CoverMode) {
        check(
            preferences.edit()
                .putString(KEY_PENDING_MODE, mode.name)
                .commit(),
        ) { "Unable to persist pending cover mode" }
    }

    fun beginTransition(mode: CoverMode, snapshot: JSONObject) {
        check(preferences.edit()
            .putString(KEY_PENDING_MODE, mode.name)
            .putString(KEY_ROLLBACK, snapshot.toString())
            .commit()) { "Unable to persist cover transition" }
    }

    fun rollbackSnapshot(): JSONObject? =
        preferences.getString(KEY_ROLLBACK, null)?.let(::JSONObject)

    fun pendingMode(): CoverMode? {
        val stored = preferences.getString(KEY_PENDING_MODE, null) ?: return null
        return CoverMode.entries.firstOrNull { it.name == stored }
    }

    fun clearPending() {
        check(preferences.edit().remove(KEY_PENDING_MODE).remove(KEY_ROLLBACK).commit()) {
            "Unable to clear pending cover mode"
        }
    }

    companion object {
        const val PREFERENCES_NAME = "cover_mode_preferences"
        private const val KEY_ACTIVE_MODE = "active_cover_mode"
        private const val KEY_ROLLBACK = "transition_rollback"
        private const val KEY_PENDING_MODE = "pending_cover_mode"
    }
}
