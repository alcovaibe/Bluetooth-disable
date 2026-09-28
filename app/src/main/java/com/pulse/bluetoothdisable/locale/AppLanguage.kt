package com.pulse.bluetoothdisable.locale

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

enum class AppLanguage(val languageTag: String) {
    RUSSIAN("ru"),
    ENGLISH("en"),
}

object AppLanguageController {
    private const val PREFS_NAME = "app_language"
    private const val KEY_LANGUAGE = "language"

    fun currentLanguage(context: Context): AppLanguage {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val appLocales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (!appLocales.isEmpty) {
                return fromLanguageTag(appLocales[0].language)
            }
        }

        val savedLanguage = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null)

        return savedLanguage?.let(::fromLanguageTag) ?: fromSystemLocale(context)
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.languageTag)
            .apply()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(language.languageTag)
        }
    }

    fun wrapContext(context: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context
        }

        val locale = Locale.forLanguageTag(currentLanguage(context).languageTag)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(configuration)
    }

    private fun fromSystemLocale(context: Context): AppLanguage {
        val locale = context.resources.configuration.locales[0]
        return fromLanguageTag(locale.language)
    }

    private fun fromLanguageTag(languageTag: String): AppLanguage =
        if (languageTag.startsWith("ru", ignoreCase = true)) {
            AppLanguage.RUSSIAN
        } else {
            AppLanguage.ENGLISH
        }
}
