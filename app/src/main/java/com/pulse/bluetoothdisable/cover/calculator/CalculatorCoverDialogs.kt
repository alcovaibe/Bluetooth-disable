package com.pulse.bluetoothdisable.cover.calculator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pulse.bluetoothdisable.R

@Composable
fun CalculatorCoverConfirmationDialog(
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    CalculatorCoverBottomSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.calculator_cover_confirm_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.calculator_cover_confirm_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        DialogActions(
            onCancel = onDismiss,
            onContinue = onContinue,
            continueEnabled = true,
        )
    }
}

@Composable
fun CalculatorCoverSetupDialog(
    onComplete: (String) -> Boolean,
    onDismiss: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var setupFailed by remember { mutableStateOf(false) }
    val codeValid = CalculatorAccessCodePolicy.isValid(code)
    val confirmationValid = CalculatorAccessCodePolicy.isValid(confirmation)
    val matches = CalculatorAccessCodePolicy.matches(code, confirmation)

    CalculatorCoverBottomSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.calculator_setup_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.calculator_setup_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
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
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        DialogActions(
            onCancel = onDismiss,
            onContinue = {
                if (!onComplete(code)) setupFailed = true
            },
            continueEnabled = codeValid && confirmationValid && matches,
        )
    }
}

@Composable
private fun CalculatorCoverBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {},
                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                    topStart = 32.dp,
                    topEnd = 32.dp,
                ),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(24.dp)
                        .fillMaxWidth(),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun DialogActions(
    onCancel: () -> Unit,
    onContinue: () -> Unit,
    continueEnabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onCancel) {
            Text(
                text = stringResource(R.string.cancel),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
        TextButton(
            onClick = onContinue,
            enabled = continueEnabled,
        ) {
            Text(
                text = stringResource(R.string.continue_action),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
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
    value.filter { it in '0'..'9' }.take(CalculatorAccessCodePolicy.CODE_LENGTH)
