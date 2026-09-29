package com.pulse.bluetoothdisable.quicksettings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Persists the last system-confirmed Quick Settings tile state.
 *
 * Android does not expose a universal "is this tile currently added" query on all
 * supported versions, so the state is maintained from TileService lifecycle callbacks
 * and, on Android 13+, from requestAddTileService() results.
 */
object TileStateStore {
    const val KEY_TILE_ADDED = "quick_settings_tile_added"

    private const val PREFERENCES_NAME = "quick_settings_tile_state"

    fun preferences(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

    fun isAdded(context: Context): Boolean =
        preferences(context).getBoolean(KEY_TILE_ADDED, false)

    fun setAdded(context: Context, added: Boolean) {
        val preferences = preferences(context)
        if (preferences.getBoolean(KEY_TILE_ADDED, false) == added) return

        preferences.edit {
            putBoolean(KEY_TILE_ADDED, added)
        }
    }
}
