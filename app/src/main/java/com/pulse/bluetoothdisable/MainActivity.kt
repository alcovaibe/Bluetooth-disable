package com.pulse.bluetoothdisable

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.pulse.bluetoothdisable.launcher.LauncherIconController
import com.pulse.bluetoothdisable.localization.LanguageManager
import com.pulse.bluetoothdisable.quicksettings.NoBluetoothTileService
import com.pulse.bluetoothdisable.quicksettings.TileStateStore
import com.pulse.bluetoothdisable.theme.ThemeManager
import com.pulse.bluetoothdisable.ui.MainScreen
import com.pulse.bluetoothdisable.ui.MainViewModel
import com.pulse.bluetoothdisable.ui.theme.BluetoothDisableTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var launcherIconController: LauncherIconController
    private var launcherIconHidden by mutableStateOf(false)
    private var quickSettingsTileAdded by mutableStateOf(false)
    private var selectedLanguage by mutableStateOf(LanguageManager.ENGLISH)
    private var selectedTheme by mutableStateOf<String?>(null)

    private val tileStateListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == TileStateStore.KEY_TILE_ADDED) {
            quickSettingsTileAdded = TileStateStore.isAdded(this)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureNavigationBarSurface()

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        launcherIconController = LauncherIconController(this)
        launcherIconHidden = launcherIconController.isHidden()
        quickSettingsTileAdded = TileStateStore.isAdded(this)
        selectedLanguage = LanguageManager.getSelectedLanguage(this)
        selectedTheme = ThemeManager.getSelectedTheme(this)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val systemDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (selectedTheme) {
                ThemeManager.LIGHT -> false
                ThemeManager.DARK -> true
                else -> systemDarkTheme
            }
            val effectiveTheme = selectedTheme ?: if (systemDarkTheme) {
                ThemeManager.DARK
            } else {
                ThemeManager.LIGHT
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
                MainScreen(
                    uiState = uiState,
                    appVersion = BuildConfig.APP_VERSION,
                    launcherIconHidden = launcherIconHidden,
                    tileAdded = quickSettingsTileAdded,
                    canRequestTile = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                    selectedLanguage = selectedLanguage,
                    selectedTheme = effectiveTheme,
                    onLanguageSelected = ::changeLanguage,
                    onThemeSelected = ::changeTheme,
                    onEnableProtection = viewModel::enableProtection,
                    onDisableProtection = viewModel::disableProtection,
                    onRefresh = viewModel::refresh,
                    onHideLauncherIcon = {
                        launcherIconController.hide()
                        launcherIconHidden = launcherIconController.isHidden()
                    },
                    onShowLauncherIcon = {
                        launcherIconController.show()
                        launcherIconHidden = launcherIconController.isHidden()
                    },
                    onRequestAddTile = ::requestQuickSettingsTile,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        TileStateStore.preferences(this).registerOnSharedPreferenceChangeListener(tileStateListener)
        quickSettingsTileAdded = TileStateStore.isAdded(this)
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.refresh()
        }
        if (::launcherIconController.isInitialized) {
            launcherIconHidden = launcherIconController.isHidden()
        }
        quickSettingsTileAdded = TileStateStore.isAdded(this)
    }

    override fun onStop() {
        TileStateStore.preferences(this).unregisterOnSharedPreferenceChangeListener(tileStateListener)
        super.onStop()
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

    private fun changeLanguage(language: String) {
        if (selectedLanguage == language) return
        LanguageManager.setSelectedLanguage(this, language)
        selectedLanguage = language
        recreate()
    }

    private fun changeTheme(theme: String) {
        if (selectedTheme == theme) return
        ThemeManager.setSelectedTheme(this, theme)
        selectedTheme = theme
    }

    private fun requestQuickSettingsTile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val statusBarManager = getSystemService(StatusBarManager::class.java)
        statusBarManager.requestAddTileService(
            ComponentName(this, NoBluetoothTileService::class.java),
            getString(R.string.qs_tile_label),
            Icon.createWithResource(this, R.drawable.ic_qs_nobluetooth),
            mainExecutor,
        ) { result ->
            // Android 13+ explicitly reports whether the tile was added or already present.
            // Errors leave the last known state untouched; lifecycle callbacks remain authoritative.
            when (result) {
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED,
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED,
                -> TileStateStore.setAdded(this, true)

                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED ->
                    TileStateStore.setAdded(this, false)
            }
        }
    }
}
