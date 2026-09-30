package com.pulse.bluetoothdisable.cover.calendar

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.pulse.bluetoothdisable.R
import com.pulse.bluetoothdisable.cover.CoverMode
import com.pulse.bluetoothdisable.cover.CoverModeManager
import com.pulse.bluetoothdisable.cover.CoverModeNavigator
import com.pulse.bluetoothdisable.ui.CoverModeConfirmationDialog
import com.pulse.bluetoothdisable.ui.coverSetupFieldShape
import com.pulse.bluetoothdisable.localization.LanguageManager
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalendarCoverSetupActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalendarCoverTheme(this) {
                CalendarSetupScreen(onCancel = ::finish) { date, text, onResult ->
                    lifecycleScope.launch {
                        val success = withContext(Dispatchers.IO) {
                            runCatching { CoverModeManager(this@CalendarCoverSetupActivity)
                                .activateCalendar(date, text) }.isSuccess
                        }
                        if (success) {
                            Toast.makeText(this@CalendarCoverSetupActivity,
                                R.string.calendar_setup_completed, Toast.LENGTH_SHORT).show()
                            CoverModeNavigator.openCover(this@CalendarCoverSetupActivity, CoverMode.CALENDAR)
                        } else {
                            onResult(false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarCoverConfirmationDialog(onContinue: () -> Unit, onDismiss: () -> Unit) {
    CoverModeConfirmationDialog(
        titleRes = R.string.calendar_cover_confirm_title,
        messageRes = R.string.calendar_cover_confirm_message,
        onContinue = onContinue,
        onDismiss = onDismiss,
    )
}

@Composable
private fun CalendarSetupScreen(
    onCancel: () -> Unit,
    onComplete: (LocalDate, String, (Boolean) -> Unit) -> Unit,
) {
    // Secrets stay only in memory during setup, never in saved-instance-state or preferences.
    var date by remember { mutableStateOf(LocalDate.now()) }
    var text by remember { mutableStateOf("") }
    var choosingDate by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    BackHandler(enabled = confirming || saving) {
        if (!saving) confirming = false
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(stringResource(R.string.calendar_setup_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.calendar_access_date), style = MaterialTheme.typography.titleMedium)
            if (confirming) {
                Text(CalendarDates.formatDate(date, locale))
                Text(stringResource(R.string.calendar_access_text), style = MaterialTheme.typography.titleMedium)
                Text(CalendarAccessPolicy.normalize(text))
                Text(stringResource(R.string.calendar_setup_instructions))
            } else {
                OutlinedButton(
                    onClick = { choosingDate = true },
                    shape = coverSetupFieldShape(),
                    modifier = Modifier.testTag("calendar_access_date_picker"),
                ) {
                    Text(CalendarDates.formatDate(date, locale))
                    Spacer(Modifier.width(8.dp))
                    Icon(painterResource(R.drawable.ic_expand_more), contentDescription = null)
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; failed = false },
                    label = { Text(stringResource(R.string.calendar_access_text)) },
                    supportingText = { Text(stringResource(R.string.calendar_text_length)) },
                    isError = text.isNotEmpty() && !CalendarAccessPolicy.isValid(text),
                    shape = coverSetupFieldShape(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (failed) Text(stringResource(R.string.calendar_setup_failed), color = MaterialTheme.colorScheme.error)
            Button(
                enabled = CalendarAccessPolicy.isValid(text) && !saving,
                onClick = {
                    if (!confirming) confirming = true else {
                        saving = true
                        failed = false
                        onComplete(date, text) { success -> saving = false; failed = !success }
                    }
                },
            ) {
                Text(stringResource(if (confirming) R.string.calendar_finish_setup else R.string.continue_action))
            }
            TextButton(enabled = !saving, onClick = {
                if (confirming) confirming = false else onCancel()
            }) { Text(stringResource(if (confirming) R.string.calendar_edit_setup else R.string.cancel)) }
        }
    }
    if (choosingDate) CalendarDateDialog(date,
        onSelect = { date = it; choosingDate = false },
        onDismiss = { choosingDate = false },
    )
}
