package com.pulse.bluetoothdisable.cover.calculator

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.pulse.bluetoothdisable.R
import com.pulse.bluetoothdisable.cover.CoverMode
import com.pulse.bluetoothdisable.cover.CoverModeManager
import com.pulse.bluetoothdisable.cover.CoverModeNavigator
import com.pulse.bluetoothdisable.localization.LanguageManager
import com.pulse.bluetoothdisable.theme.ThemeManager
import com.pulse.bluetoothdisable.ui.theme.BluetoothDisableTheme

class CalculatorCoverSetupActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureNavigationBarSurface()

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
                CalculatorCoverSetupScreen(
                    onCancel = { finish() },
                    onComplete = ::completeSetup,
                )
            }
        }
    }

    private fun completeSetup(code: String): Boolean = try {
        CoverModeManager(this).activateCalculator(code)
        Toast.makeText(this, R.string.calculator_setup_completed, Toast.LENGTH_SHORT).show()
        CoverModeNavigator.hideToCoverMode(this, CoverMode.CALCULATOR)
        true
    } catch (_: Exception) {
        false
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

@Composable
private fun CalculatorCoverSetupScreen(
    onCancel: () -> Unit,
    onComplete: (String) -> Boolean,
) {
    var code by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var setupFailed by remember { mutableStateOf(false) }
    val codeValid = CalculatorAccessCodePolicy.isValid(code)
    val confirmationValid = CalculatorAccessCodePolicy.isValid(confirmation)
    val matches = CalculatorAccessCodePolicy.matches(code, confirmation)

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.calculator_setup_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.calculator_setup_description),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            )

            AccessCodeField(
                value = code,
                label = stringResource(R.string.calculator_access_code),
                onValueChange = {
                    code = sanitizeCode(it)
                    setupFailed = false
                },
                showLengthError = code.isNotEmpty() && !codeValid,
            )
            AccessCodeField(
                value = confirmation,
                label = stringResource(R.string.calculator_repeat_code),
                onValueChange = {
                    confirmation = sanitizeCode(it)
                    setupFailed = false
                },
                showLengthError = confirmation.isNotEmpty() && !confirmationValid,
                mismatch = confirmationValid && codeValid && code != confirmation,
            )

            if (setupFailed) {
                Text(
                    text = stringResource(R.string.calculator_setup_failed),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(modifier = Modifier.padding(top = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        if (!onComplete(code)) setupFailed = true
                    },
                    enabled = codeValid && confirmationValid && matches,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(stringResource(R.string.continue_action))
                }
            }
        }
    }
}

@Composable
private fun AccessCodeField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    showLengthError: Boolean,
    mismatch: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Next,
        ),
        singleLine = true,
        isError = showLengthError || mismatch,
        supportingText = {
            when {
                mismatch -> Text(stringResource(R.string.calculator_code_mismatch))
                showLengthError -> Text(stringResource(R.string.calculator_code_length_error))
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    )
}

private fun sanitizeCode(value: String): String =
    value.filter(Char::isDigit).take(CalculatorAccessCodePolicy.CODE_LENGTH)
