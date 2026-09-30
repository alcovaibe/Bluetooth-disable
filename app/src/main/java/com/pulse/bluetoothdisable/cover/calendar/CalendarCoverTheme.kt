package com.pulse.bluetoothdisable.cover.calendar

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.pulse.bluetoothdisable.theme.ThemeManager
import com.pulse.bluetoothdisable.ui.theme.BluetoothDisableTheme

@Composable
internal fun CalendarCoverTheme(activity: Activity, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (ThemeManager.getSelectedTheme(activity)) {
        ThemeManager.LIGHT -> false
        ThemeManager.DARK -> true
        else -> systemDark
    }
    val view = LocalView.current
    SideEffect {
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
    BluetoothDisableTheme(darkTheme = darkTheme, content = content)
}
