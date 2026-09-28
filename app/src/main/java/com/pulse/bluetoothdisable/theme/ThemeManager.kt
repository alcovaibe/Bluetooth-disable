package com.pulse.bluetoothdisable.theme

import android.content.Context

object ThemeManager {
    const val LIGHT = "light"
    const val DARK = "dark"

    private const val PREFS_NAME = "theme_preferences"
    private const val KEY_THEME = "theme"

    fun getSelectedTheme(context: Context): String? {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, null)
        return when (stored) {
            LIGHT, DARK -> stored
            else -> null
        }
    }

    fun setSelectedTheme(context: Context, theme: String) {
        require(theme == LIGHT || theme == DARK)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, theme)
            .apply()
    }
}
