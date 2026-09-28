package com.pulse.bluetoothdisable.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pulse.bluetoothdisable.R
import com.pulse.bluetoothdisable.domain.ProtectionError
import com.pulse.bluetoothdisable.domain.ProtectionState
import com.pulse.bluetoothdisable.localization.LanguageManager
import com.pulse.bluetoothdisable.ui.theme.BluetoothDisableTheme

@Composable
fun MainScreen(
    uiState: ProtectionUiState,
    launcherIconHidden: Boolean,
    canRequestTile: Boolean,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onEnableProtection: () -> Unit,
    onDisableProtection: () -> Unit,
    onRefresh: () -> Unit,
    onHideLauncherIcon: () -> Unit,
    onShowLauncherIcon: () -> Unit,
    onRequestAddTile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showHideDialog by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            LanguageSwitcher(
                selectedLanguage = selectedLanguage,
                onLanguageSelected = onLanguageSelected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 12.dp)
                    .zIndex(1f),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, top = 72.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.protection_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(32.dp))

                val statusText = when (uiState.state) {
                    ProtectionState.NOT_PROVISIONED -> null
                    ProtectionState.READY -> R.string.status_ready
                    ProtectionState.ENABLING -> R.string.status_enabling
                    ProtectionState.PROTECTED -> R.string.status_protected
                    ProtectionState.DISABLING -> R.string.status_disabling
                    ProtectionState.ERROR -> R.string.status_error
                    ProtectionState.UNSUPPORTED -> R.string.status_unsupported
                }
                if (statusText != null) {
                    Text(
                        text = stringResource(statusText),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                val messageText = when (uiState.state) {
                    ProtectionState.NOT_PROVISIONED -> stringResource(R.string.message_not_provisioned)
                    ProtectionState.READY -> stringResource(R.string.message_ready)
                    ProtectionState.ENABLING -> stringResource(R.string.message_enabling)
                    ProtectionState.PROTECTED -> stringResource(R.string.message_protected)
                    ProtectionState.DISABLING -> stringResource(R.string.message_disabling)
                    ProtectionState.ERROR -> stringResource(errorMessageResource(uiState.error))
                    ProtectionState.UNSUPPORTED -> stringResource(R.string.message_unsupported)
                }
                Text(
                    text = messageText,
                    style = MaterialTheme.typography.bodyLarge,
                )

                if (uiState.state == ProtectionState.ENABLING ||
                    uiState.state == ProtectionState.DISABLING
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator()
                }

                Spacer(modifier = Modifier.height(32.dp))

                when (uiState.state) {
                    ProtectionState.READY -> ActionButton(
                        text = stringResource(R.string.action_enable),
                        onClick = onEnableProtection,
                    )

                    ProtectionState.PROTECTED -> ActionButton(
                        text = stringResource(R.string.action_disable),
                        onClick = onDisableProtection,
                    )

                    ProtectionState.NOT_PROVISIONED,
                    ProtectionState.ERROR -> ActionButton(
                        text = stringResource(R.string.action_refresh),
                        onClick = onRefresh,
                    )

                    ProtectionState.ENABLING,
                    ProtectionState.DISABLING,
                    ProtectionState.UNSUPPORTED -> Unit
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = stringResource(R.string.launcher_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (launcherIconHidden) {
                    Text(
                        text = stringResource(R.string.launcher_hidden),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (canRequestTile) {
                    OutlinedButton(
                        onClick = onRequestAddTile,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.action_add_tile))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Text(
                        text = stringResource(R.string.tile_manual_hint),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedButton(
                    onClick = {
                        if (launcherIconHidden) {
                            onShowLauncherIcon()
                        } else {
                            showHideDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(
                            if (launcherIconHidden) {
                                R.string.action_show_launcher
                            } else {
                                R.string.action_hide_launcher
                            },
                        ),
                    )
                }
            }
        }
    }

    if (showHideDialog) {
        AlertDialog(
            onDismissRequest = { showHideDialog = false },
            title = { Text(stringResource(R.string.hide_launcher_dialog_title)) },
            text = { Text(stringResource(R.string.hide_launcher_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showHideDialog = false
                        onHideLauncherIcon()
                    },
                ) {
                    Text(stringResource(R.string.hide_launcher_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showHideDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun LanguageSwitcher(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LanguageButton(
            label = "RU",
            selected = selectedLanguage == LanguageManager.RUSSIAN,
            onClick = { onLanguageSelected(LanguageManager.RUSSIAN) },
        )
        Text(
            text = "|",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LanguageButton(
            label = "EN",
            selected = selectedLanguage == LanguageManager.ENGLISH,
            onClick = { onLanguageSelected(LanguageManager.ENGLISH) },
        )
    }
}

@Composable
private fun LanguageButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Text(
            text = label,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

private fun errorMessageResource(error: ProtectionError?): Int = when (error) {
    ProtectionError.READ_POLICY -> R.string.error_read_policy
    ProtectionError.ENABLE_NOT_CONFIRMED -> R.string.error_enable_not_confirmed
    ProtectionError.ENABLE_FAILED -> R.string.error_enable_failed
    ProtectionError.DISABLE_NOT_CONFIRMED -> R.string.error_disable_not_confirmed
    ProtectionError.DISABLE_FAILED -> R.string.error_disable_failed
    null -> R.string.message_error
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = text)
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    BluetoothDisableTheme {
        MainScreen(
            uiState = ProtectionUiState(state = ProtectionState.READY),
            launcherIconHidden = false,
            canRequestTile = true,
            selectedLanguage = LanguageManager.ENGLISH,
            onLanguageSelected = {},
            onEnableProtection = {},
            onDisableProtection = {},
            onRefresh = {},
            onHideLauncherIcon = {},
            onShowLauncherIcon = {},
            onRequestAddTile = {},
        )
    }
}
