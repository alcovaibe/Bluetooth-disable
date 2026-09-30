package com.pulse.bluetoothdisable.cover.calculator

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.pulse.bluetoothdisable.MainActivity
import com.pulse.bluetoothdisable.R
import com.pulse.bluetoothdisable.cover.CoverDeviceAuthenticator
import com.pulse.bluetoothdisable.cover.CoverMode
import com.pulse.bluetoothdisable.cover.CoverModeManager
import com.pulse.bluetoothdisable.cover.CoverModeNavigator
import com.pulse.bluetoothdisable.cover.CoverRecoveryManager
import com.pulse.bluetoothdisable.localization.LanguageManager
import com.pulse.bluetoothdisable.theme.ThemeManager
import com.pulse.bluetoothdisable.ui.theme.BluetoothDisableTheme

class CalculatorCoverActivity : FragmentActivity() {
    private lateinit var recovery: CoverRecoveryManager
    private lateinit var authenticator: CoverDeviceAuthenticator
    private var recoveryState by mutableStateOf(CoverRecoveryManager.State.IDLE)
    private var resumed by mutableStateOf(false)
    private lateinit var viewModel: CalculatorViewModel

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

        val modes = CoverModeManager(this)
        recovery = CoverRecoveryManager(
            activeMode = modes::activeMode,
            resetCover = modes::resetCalculatorCover,
            onStateChanged = { recoveryState = it },
        )
        authenticator = CoverDeviceAuthenticator(this)
        enableEdgeToEdge()
        configureNavigationBarSurface()
        viewModel = ViewModelProvider(this)[CalculatorViewModel::class.java]

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
                    recoveryEnabled = resumed && recoveryState == CoverRecoveryManager.State.IDLE,
                    onRecoveryHold = ::beginRecovery,
                    onUnlock = {
                        CoverModeNavigator.openMainFromCover(this, CoverMode.CALCULATOR)
                    },
                )
                if (recoveryState == CoverRecoveryManager.State.CONFIRMING) {
                    AlertDialog(
                        onDismissRequest = recovery::cancel,
                        title = { Text(stringResource(R.string.cover_recovery_title)) },
                        text = { Text(stringResource(R.string.calculator_recovery_description)) },
                        dismissButton = {
                            TextButton(onClick = recovery::cancel) {
                                Text(stringResource(R.string.cancel))
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = ::resetCover) {
                                Text(stringResource(R.string.cover_recovery_reset))
                            }
                        },
                    )
                }
            }
        }
    }

    private fun beginRecovery() {
        if (!resumed) return
        val attempt = recovery.begin() ?: return
        window.decorView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        val started = authenticator.authenticate { success ->
            if (success) recovery.authenticationSucceeded(attempt)
            else recovery.authenticationRejected(attempt)
        }
        if (!started) {
            recovery.authenticationRejected(attempt)
            Toast.makeText(this, R.string.cover_recovery_auth_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    private fun resetCover() {
        try {
            if (recovery.confirmReset()) CoverModeNavigator.openDefaultMain(this)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.cover_recovery_error, Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
    }

    override fun onPause() {
        resumed = false // Cancels a pointer timer even if Android did not send touch CANCEL.
        if (::recovery.isInitialized && recovery.state == CoverRecoveryManager.State.CONFIRMING) {
            recovery.cancel()
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (::recovery.isInitialized) recovery.cancel()
        if (::authenticator.isInitialized) authenticator.close()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::recovery.isInitialized) recovery.cancel()
        if (::viewModel.isInitialized) {
            viewModel.clear()
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
