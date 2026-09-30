package com.pulse.bluetoothdisable.cover.calendar

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val month: YearMonth = YearMonth.from(selectedDate),
    val notes: List<CalendarNote> = emptyList(),
    val loading: Boolean = true,
    val storageError: Boolean = false,
    val editorOpen: Boolean = false,
    val editingNote: CalendarNote? = null,
    val busy: Boolean = false,
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CalendarNotesRepository = LocalCalendarNotesRepository(application)
    private val access = CalendarAccessManager(application)
    var uiState by mutableStateOf(CalendarUiState())
        private set

    init { refresh() }

    fun select(date: LocalDate) {
        uiState = uiState.copy(selectedDate = date, month = YearMonth.from(date))
    }

    fun moveMonth(amount: Long) {
        val month = uiState.month.plusMonths(amount)
        if (month.year !in CalendarDates.YEAR_RANGE) return
        select(month.atDay(uiState.selectedDate.dayOfMonth.coerceAtMost(month.lengthOfMonth())))
    }

    fun today() = select(LocalDate.now())

    fun add() { uiState = uiState.copy(editorOpen = true, editingNote = null) }
    fun edit(note: CalendarNote) { uiState = uiState.copy(editorOpen = true, editingNote = note) }
    fun closeEditor() {
        if (!uiState.busy) uiState = uiState.copy(editorOpen = false, editingNote = null)
    }

    fun click(note: CalendarNote, onUnlock: () -> Unit) {
        if (uiState.busy) return
        uiState = uiState.copy(busy = true)
        viewModelScope.launch {
            val matches = withContext(Dispatchers.IO) { access.verify(note.date, note.text) }
            uiState = uiState.copy(busy = false)
            if (matches) onUnlock() else edit(note)
        }
    }

    fun save(text: String) {
        if (text.isBlank() || uiState.busy) return
        val date = uiState.editingNote?.date ?: uiState.selectedDate
        val id = uiState.editingNote?.id
        mutate { repository.save(date, text, id) }
    }

    fun delete(note: CalendarNote) = mutate { repository.delete(note.id) }

    private fun mutate(action: () -> Unit) {
        if (uiState.busy) return
        uiState = uiState.copy(busy = true)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { action(); repository.notes() } }
            uiState = if (result.isSuccess) uiState.copy(
                notes = result.getOrThrow(), busy = false, storageError = false,
                editorOpen = false, editingNote = null,
            ) else uiState.copy(busy = false, storageError = true)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { repository.notes() } }
            uiState = uiState.copy(
                notes = result.getOrDefault(emptyList()), loading = false, storageError = result.isFailure,
            )
        }
    }
}
