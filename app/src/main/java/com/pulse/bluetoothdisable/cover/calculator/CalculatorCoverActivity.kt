package com.pulse.bluetoothdisable.cover.calculator

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.pulse.bluetoothdisable.MainActivity
import com.pulse.bluetoothdisable.cover.CoverMode
import com.pulse.bluetoothdisable.cover.CoverModeManager
import com.pulse.bluetoothdisable.cover.CoverModeNavigator
import com.pulse.bluetoothdisable.localization.LanguageManager
import com.pulse.bluetoothdisable.theme.ThemeManager
import com.pulse.bluetoothdisable.ui.theme.BluetoothDisableTheme

class CalculatorCoverActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!CoverModeManager(this).isCalculatorReady()) {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                },
            )
            finish()
            return
        }

        enableEdgeToEdge()
        configureNavigationBarSurface()
        val viewModel = ViewModelProvider(this)[CalculatorViewModel::class.java]

        setContent {
            val systemDark = isSystemInDarkTheme()
            val selectedTheme = ThemeManager.getSelectedTheme(this)
            val darkTheme = when (selectedTheme) {
                ThemeManager.LIGHT -> false
                ThemeManager.DARK -> true
                else -> systemDark
            }
            val view = LocalView.current
            SideEffect {
                configureNavigationBarSurface()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            BluetoothDisableTheme(darkTheme = darkTheme) {
                CalculatorScreen(
                    viewModel = viewModel,
                    onUnlock = {
                        CoverModeNavigator.openMainFromCover(this, CoverMode.CALCULATOR)
                    },
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun configureNavigationBarSurface() {
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.navigationBarDividerColor = Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }
}
