package com.pulse.bluetoothdisable.localization

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

object LanguageManager {
    const val ENGLISH = "en"
    const val RUSSIAN = "ru"

    private const val PREFS_NAME = "language_preferences"
    private const val KEY_LANGUAGE = "language"

    fun getSelectedLanguage(context: Context): String {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null)
        if (stored == ENGLISH || stored == RUSSIAN) return stored

        val systemLanguage = context.resources.configuration.locales[0].language
        return if (systemLanguage == RUSSIAN) RUSSIAN else ENGLISH
    }

    fun setSelectedLanguage(context: Context, language: String) {
        require(language == ENGLISH || language == RUSSIAN)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_LANGUAGE, language)
        }
    }

    fun wrapContext(context: Context): Context {
        val language = getSelectedLanguage(context)
        val locale = Locale.forLanguageTag(language)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(locale))
        }
        return context.createConfigurationContext(configuration)
    }
}
