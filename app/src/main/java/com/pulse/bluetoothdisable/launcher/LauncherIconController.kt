package com.pulse.bluetoothdisable.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.edit

enum class LauncherStyle(
    val preferenceValue: String,
    internal val aliasSuffix: String,
    internal val enabledByDefault: Boolean = false,
) {
    DEFAULT("default", ".LauncherAlias", enabledByDefault = true),
    CALCULATOR("calculator", ".LauncherAliasCalculator"),
    NOTES("notes", ".LauncherAliasNotes"),
    CALENDAR("calendar", ".LauncherAliasCalendar"),
    GALLERY("gallery", ".LauncherAliasGallery"),
}

class LauncherIconController(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val preferences = appContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun isHidden(): Boolean = LauncherStyle.values().none(::isEnabled)

    fun selectedStyle(): LauncherStyle {
        val stored = storedStyle()
        if (isEnabled(stored)) return stored

        return LauncherStyle.values().firstOrNull(::isEnabled) ?: stored
    }

    fun setStyle(style: LauncherStyle) {
        preferences.edit(commit = true) {
            putString(KEY_SELECTED_STYLE, style.preferenceValue)
        }
        activate(style)
    }

    fun hide() {
        LauncherStyle.values().forEach { style ->
            setEnabled(style, false)
        }
    }

    fun show() {
        activate(storedStyle())
    }

    private fun activate(style: LauncherStyle) {
        // Enable the replacement first so the launcher is never intentionally left
        // without an entry during an icon/name switch.
        setEnabled(style, true)
        LauncherStyle.values()
            .filterNot { it == style }
            .forEach { other -> setEnabled(other, false) }
    }

    private fun storedStyle(): LauncherStyle {
        val value = preferences.getString(KEY_SELECTED_STYLE, null)
        return LauncherStyle.values().firstOrNull { it.preferenceValue == value }
            ?: LauncherStyle.DEFAULT
    }

    private fun isEnabled(style: LauncherStyle): Boolean =
        when (packageManager.getComponentEnabledSetting(component(style))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> style.enabledByDefault
            else -> false
        }

    private fun setEnabled(style: LauncherStyle, enabled: Boolean) {
        packageManager.setComponentEnabledSetting(
            component(style),
            if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            },
            PackageManager.DONT_KILL_APP,
        )
    }

    private fun component(style: LauncherStyle): ComponentName =
        ComponentName(
            appContext,
            "${appContext.packageName}${style.aliasSuffix}",
        )

    companion object {
        const val PREFERENCES_NAME = "launcher_icon_preferences"
        private const val KEY_SELECTED_STYLE = "selected_launcher_style"
    }
}
