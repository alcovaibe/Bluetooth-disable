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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val editorDate: LocalDate? = null,
    val editorText: String = "",
    val viewingNote: CalendarNote? = null,
    val noteError: CalendarNotePolicy.Violation? = null,
    val busy: Boolean = false,
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CalendarNotesRepository = LocalCalendarNotesRepository(application)
    private val access = CalendarAccessManager(application)
    private val drafts = CalendarDraftStore(application)
    private var draftWriteJob: Job? = null

    var uiState by mutableStateOf(CalendarUiState())
        private set

    init {
        refresh()
    }

    fun select(date: LocalDate) {
        uiState = uiState.copy(
            selectedDate = date,
            month = YearMonth.from(date),
            noteError = null,
        )
    }

    fun moveMonth(amount: Long) {
        val month = uiState.month.plusMonths(amount)
        if (month.year !in CalendarDates.YEAR_RANGE) return
        select(month.atDay(1))
    }

    fun today() = select(LocalDate.now())

    fun add() {
        if (uiState.busy) return
        if (uiState.notes.count { it.date == uiState.selectedDate } >= CalendarNotePolicy.MAX_NOTES_PER_DATE) {
            uiState = uiState.copy(noteError = CalendarNotePolicy.Violation.DATE_LIMIT)
            return
        }
        openEditor(note = null, date = uiState.selectedDate)
    }

    fun edit(note: CalendarNote) {
        if (uiState.busy) return
        openEditor(note = note, date = note.date)
    }

    private fun openEditor(note: CalendarNote?, date: LocalDate) {
        uiState = uiState.copy(busy = true, noteError = null)
        viewModelScope.launch {
            val draft = withContext(Dispatchers.IO) {
                runCatching { drafts.text(date, note?.id) }
            }
            uiState = if (draft.isSuccess) {
                uiState.copy(
                    busy = false,
                    editorOpen = true,
                    editingNote = note,
                    editorDate = date,
                    editorText = draft.getOrNull() ?: note?.text.orEmpty(),
                    noteError = null,
                )
            } else {
                uiState.copy(busy = false, storageError = true)
            }
        }
    }

    fun updateEditorText(value: String) {
        if (!uiState.editorOpen || uiState.busy) return
        val limited = value.take(CalendarNotePolicy.MAX_TEXT_LENGTH)
        uiState = uiState.copy(
            editorText = limited,
            noteError = if (value.length > CalendarNotePolicy.MAX_TEXT_LENGTH) {
                CalendarNotePolicy.Violation.TOO_LONG
            } else {
                null
            },
        )
        scheduleDraftWrite()
    }

    fun closeEditor() {
        if (uiState.busy || !uiState.editorOpen) return
        persistDraftNow()
        uiState = uiState.copy(
            editorOpen = false,
            editingNote = null,
            editorDate = null,
            editorText = "",
            noteError = null,
        )
    }

    fun persistOpenDraft() {
        if (uiState.editorOpen && !uiState.busy) persistDraftNow()
    }

    fun closeViewer() {
        uiState = uiState.copy(viewingNote = null)
    }

    fun clearNoteError() {
        uiState = uiState.copy(noteError = null)
    }

    fun click(note: CalendarNote, onUnlock: () -> Unit) {
        if (uiState.busy) return
        uiState = uiState.copy(busy = true, noteError = null)
        viewModelScope.launch {
            val matches = withContext(Dispatchers.IO) { access.verify(note.date, note.text) }
            uiState = uiState.copy(busy = false)
            if (matches) {
                onUnlock()
            } else {
                uiState = uiState.copy(viewingNote = note)
            }
        }
    }

    fun save() {
        if (uiState.busy || !uiState.editorOpen) return
        val date = uiState.editorDate ?: return
        val editingId = uiState.editingNote?.id
        val text = uiState.editorText
        val violation = CalendarNotePolicy.validate(date, text, uiState.notes, editingId)
        if (violation != null) {
            uiState = uiState.copy(noteError = violation)
            return
        }

        draftWriteJob?.cancel()
        uiState = uiState.copy(busy = true, noteError = null)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    repository.save(date, text, editingId)
                    drafts.remove(date, editingId)
                    repository.notes()
                }
            }
            val error = result.exceptionOrNull()
            uiState = when {
                result.isSuccess -> uiState.copy(
                    notes = result.getOrThrow(),
                    busy = false,
                    storageError = false,
                    editorOpen = false,
                    editingNote = null,
                    editorDate = null,
                    editorText = "",
                    noteError = null,
                )
                error is CalendarNoteValidationException -> uiState.copy(
                    busy = false,
                    noteError = error.violation,
                )
                else -> uiState.copy(busy = false, storageError = true)
            }
        }
    }

    fun delete(note: CalendarNote) {
        if (uiState.busy) return
        uiState = uiState.copy(busy = true, noteError = null)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    repository.delete(note.id)
                    drafts.remove(note.date, note.id)
                    repository.notes()
                }
            }
            uiState = if (result.isSuccess) {
                uiState.copy(
                    notes = result.getOrThrow(),
                    busy = false,
                    storageError = false,
                    viewingNote = uiState.viewingNote?.takeUnless { it.id == note.id },
                )
            } else {
                uiState.copy(busy = false, storageError = true)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { repository.notes() } }
            uiState = if (result.isSuccess) {
                uiState.copy(
                    notes = result.getOrThrow(),
                    loading = false,
                    storageError = false,
                )
            } else {
                uiState.copy(loading = false, storageError = true)
            }
        }
    }

    private fun scheduleDraftWrite() {
        val snapshot = draftSnapshot() ?: return
        draftWriteJob?.cancel()
        draftWriteJob = viewModelScope.launch {
            delay(DRAFT_WRITE_DELAY_MILLIS)
            withContext(Dispatchers.IO) { writeDraft(snapshot) }
        }
    }

    private fun persistDraftNow() {
        val snapshot = draftSnapshot() ?: return
        draftWriteJob?.cancel()
        draftWriteJob = viewModelScope.launch {
            withContext(Dispatchers.IO) { writeDraft(snapshot) }
        }
    }

    private fun draftSnapshot(): DraftSnapshot? {
        if (!uiState.editorOpen) return null
        val date = uiState.editorDate ?: return null
        return DraftSnapshot(date, uiState.editingNote, uiState.editorText)
    }

    private fun writeDraft(snapshot: DraftSnapshot) {
        val unchanged = snapshot.note != null && snapshot.text == snapshot.note.text
        if (snapshot.text.isEmpty() || unchanged) {
            drafts.remove(snapshot.date, snapshot.note?.id)
        } else {
            drafts.put(snapshot.date, snapshot.note?.id, snapshot.text)
        }
    }

    private data class DraftSnapshot(
        val date: LocalDate,
        val note: CalendarNote?,
        val text: String,
    )

    companion object {
        private const val DRAFT_WRITE_DELAY_MILLIS = 250L
    }
}
