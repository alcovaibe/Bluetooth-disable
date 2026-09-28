package com.pulse.bluetoothdisable.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pulse.bluetoothdisable.R
import com.pulse.bluetoothdisable.domain.ProtectionState
import com.pulse.bluetoothdisable.ui.theme.BluetoothDisableTheme

@Composable
fun MainScreen(
    uiState: ProtectionUiState,
    onEnableProtection: () -> Unit,
    onDisableProtection: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
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
                ProtectionState.NOT_PROVISIONED -> R.string.status_not_provisioned
                ProtectionState.READY -> R.string.status_ready
                ProtectionState.ENABLING -> R.string.status_enabling
                ProtectionState.PROTECTED -> R.string.status_protected
                ProtectionState.DISABLING -> R.string.status_disabling
                ProtectionState.ERROR -> R.string.status_error
                ProtectionState.UNSUPPORTED -> R.string.status_unsupported
            }
            Text(
                text = stringResource(statusText),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            val messageText = when (uiState.state) {
                ProtectionState.NOT_PROVISIONED -> stringResource(R.string.message_not_provisioned)
                ProtectionState.READY -> stringResource(R.string.message_ready)
                ProtectionState.ENABLING -> stringResource(R.string.message_enabling)
                ProtectionState.PROTECTED -> stringResource(R.string.message_protected)
                ProtectionState.DISABLING -> stringResource(R.string.message_disabling)
                ProtectionState.ERROR -> uiState.errorMessage
                    ?: stringResource(R.string.message_error)
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
        }
    }
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
            onEnableProtection = {},
            onDisableProtection = {},
            onRefresh = {},
        )
    }
}
