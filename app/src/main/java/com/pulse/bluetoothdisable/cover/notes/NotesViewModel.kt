package com.pulse.bluetoothdisable.cover.notes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NotesUiState(
    val notes: List<LocalNote> = emptyList(),
    val loading: Boolean = true,
    val storageError: Boolean = false,
    val selectedNoteId: String? = null,
    val editorOpen: Boolean = false,
    val editingNote: LocalNote? = null,
    val busy: Boolean = false,
)

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalNotesRepository(application)
    private val access = NotesAccessManager(application)
    private val language = application.resources.configuration.locales[0].language

    var uiState by mutableStateOf(NotesUiState())
        private set

    init {
        refresh()
    }

    fun open(note: LocalNote) {
        uiState = uiState.copy(selectedNoteId = note.id)
    }

    fun closeNote() {
        uiState = uiState.copy(selectedNoteId = null)
    }

    fun add() {
        uiState = uiState.copy(editorOpen = true, editingNote = null)
    }

    fun edit(note: LocalNote) {
        uiState = uiState.copy(editorOpen = true, editingNote = note)
    }

    fun closeEditor() {
        if (!uiState.busy) uiState = uiState.copy(editorOpen = false, editingNote = null)
    }

    fun save(title: String, body: String) {
        if (!NotesPolicy.isValid(title, body) || uiState.busy) return
        val editing = uiState.editingNote
        mutate {
            val updated = repository.save(title, body, editing?.id, editing?.pinned)
            if (editing != null && access.isSecretNote(editing.id) &&
                !access.noteStillMatches(updated.id, updated.body)
            ) {
                // Do not move a hidden trigger silently after an edit. The note remains;
                // only this Notes Cover session loses its hidden access rule.
                access.clear()
            }
        }
    }

    fun delete(note: LocalNote) = mutate {
        repository.delete(note.id)
        if (access.isSecretNote(note.id)) access.clear()
    }

    fun setPinned(note: LocalNote, pinned: Boolean) = mutate {
        repository.setPinned(note.id, pinned)
    }

    fun generate(count: Int) {
        if (uiState.busy || count !in setOf(2, 5, 7, 10)) return
        mutate {
            NotesGenerator.generate(count, language).forEach { generated ->
                repository.save(generated.title, generated.body)
            }
        }
    }

    fun tap(note: LocalNote, offset: Int, onUnlock: () -> Unit) {
        if (uiState.busy) return
        viewModelScope.launch {
            val matches = withContext(Dispatchers.IO) {
                access.matchesTap(note.id, note.body, offset)
            }
            if (matches) onUnlock()
        }
    }

    private fun mutate(action: () -> Unit) {
        if (uiState.busy) return
        uiState = uiState.copy(busy = true)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    action()
                    repository.notes()
                }
            }
            uiState = if (result.isSuccess) {
                val notes = result.getOrThrow()
                val selected = uiState.selectedNoteId?.takeIf { id -> notes.any { it.id == id } }
                uiState.copy(
                    notes = notes,
                    selectedNoteId = selected,
                    busy = false,
                    storageError = false,
                    editorOpen = false,
                    editingNote = null,
                )
            } else {
                uiState.copy(busy = false, storageError = true)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { repository.notes() } }
            uiState = uiState.copy(
                notes = result.getOrDefault(emptyList()),
                loading = false,
                storageError = result.isFailure,
            )
        }
    }

    fun resetTransientUi() {
        uiState = uiState.copy(selectedNoteId = null, editorOpen = false, editingNote = null)
    }
}
