package com.pulse.bluetoothdisable.cover.calculator

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.bluetoothdisable.R
import com.pulse.bluetoothdisable.ui.CoverModeBottomSheet
import com.pulse.bluetoothdisable.ui.CoverModeConfirmationDialog
import com.pulse.bluetoothdisable.ui.CoverModeDialogActions
import com.pulse.bluetoothdisable.ui.coverSetupFieldShape

@Composable
fun CalculatorCoverConfirmationDialog(
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    CoverModeConfirmationDialog(
        titleRes = R.string.calculator_cover_confirm_title,
        messageRes = R.string.calculator_cover_confirm_message,
        onContinue = onContinue,
        onDismiss = onDismiss,
    )
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

    CoverModeBottomSheet(onDismiss = onDismiss) {
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

        CoverModeDialogActions(
            onCancel = onDismiss,
            onContinue = {
                if (!onComplete(code)) setupFailed = true
            },
            continueEnabled = codeValid && confirmationValid && matches,
        )
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
        shape = coverSetupFieldShape(),
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
