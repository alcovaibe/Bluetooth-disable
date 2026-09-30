package com.pulse.bluetoothdisable.cover.calendar

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pulse.bluetoothdisable.R
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarDateDialog(date: LocalDate, onSelect: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = CalendarDates.toPickerMillis(date),
        yearRange = CalendarDates.YEAR_RANGE,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    state.selectedDateMillis?.let { onSelect(CalendarDates.fromPickerMillis(it)) }
                },
            ) { Text(stringResource(R.string.calendar_select)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    ) {
        // The built-in input toggle accepts a distant date directly, without month paging.
        DatePicker(state = state, title = { Text(stringResource(R.string.calendar_choose_date), Modifier.padding(start = 24.dp, top = 16.dp)) })
    }
}
