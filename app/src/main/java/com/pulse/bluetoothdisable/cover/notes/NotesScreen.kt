package com.pulse.bluetoothdisable.cover.notes

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulse.bluetoothdisable.R
import com.pulse.bluetoothdisable.cover.coverRecoveryHold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    recoveryEnabled: Boolean = false,
    onRecoveryHold: () -> Unit = {},
    onUnlock: () -> Unit,
) {
    val state = viewModel.uiState
    val selected = state.selectedNoteId?.let { id -> state.notes.firstOrNull { it.id == id } }
    val inlineEditing = selected != null &&
        state.editorOpen &&
        state.editingNote?.id == selected.id
    var inlineTitle by remember(selected?.id, inlineEditing) {
        mutableStateOf(selected?.title.orEmpty())
    }
    var inlineBody by remember(selected?.id, inlineEditing) {
        mutableStateOf(selected?.body.orEmpty())
    }
    var deleting by remember { mutableStateOf<LocalNote?>(null) }
    var showGenerate by remember { mutableStateOf(false) }

    if (selected == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.launcher_name_notes),
                            modifier = Modifier.coverRecoveryHold(recoveryEnabled, onRecoveryHold),
                        )
                    },
                    actions = {
                        TextButton(onClick = { showGenerate = true }, enabled = !state.busy) {
                            Text(stringResource(R.string.notes_generate))
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = viewModel::add) {
                    Text("+", style = MaterialTheme.typography.headlineMedium)
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when {
                    state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                    state.storageError -> Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.notes_storage_error), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = viewModel::refresh) { Text(stringResource(R.string.notes_retry)) }
                    }
                    state.notes.isEmpty() -> Text(
                        stringResource(R.string.notes_empty),
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.notes, key = { it.id }) { note ->
                            NoteListCard(note = note, onClick = { viewModel.open(note) })
                        }
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        TextButton(
                            onClick = {
                                if (inlineEditing) viewModel.closeEditor() else viewModel.closeNote()
                            },
                            enabled = !state.busy,
                        ) { Text("‹") }
                    },
                    title = {
                        Text(
                            (if (inlineEditing) inlineTitle else selected.title)
                                .ifBlank { stringResource(R.string.notes_note) },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    actions = {
                        if (inlineEditing) {
                            TextButton(onClick = viewModel::closeEditor, enabled = !state.busy) {
                                Text(stringResource(R.string.cancel))
                            }
                            TextButton(
                                onClick = { viewModel.save(inlineTitle, inlineBody) },
                                enabled = NotesPolicy.isValid(inlineTitle, inlineBody) && !state.busy,
                            ) {
                                Text(stringResource(R.string.notes_save))
                            }
                        } else {
                            TextButton(
                                onClick = { viewModel.setPinned(selected, !selected.pinned) },
                                enabled = !state.busy,
                            ) {
                                Text(stringResource(if (selected.pinned) R.string.notes_unpin else R.string.notes_pin))
                            }
                            TextButton(onClick = { viewModel.edit(selected) }, enabled = !state.busy) {
                                Text(stringResource(R.string.notes_edit))
                            }
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (inlineEditing) {
                    OutlinedTextField(
                        value = inlineTitle,
                        onValueChange = {
                            if (it.length <= NotesPolicy.MAX_TITLE_LENGTH) inlineTitle = it
                        },
                        enabled = !state.busy,
                        label = { Text(stringResource(R.string.notes_title_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = inlineBody,
                        onValueChange = {
                            if (it.length <= NotesPolicy.MAX_BODY_LENGTH) inlineBody = it
                        },
                        enabled = !state.busy,
                        label = { Text(stringResource(R.string.notes_body_label)) },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                    if (state.storageError) {
                        Text(
                            stringResource(R.string.notes_storage_error),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    if (selected.title.isNotBlank()) {
                        Text(selected.title, style = MaterialTheme.typography.headlineSmall)
                    }
                    SecretAwareBody(
                        text = selected.body,
                        onTap = { offset -> viewModel.tap(selected, offset, onUnlock) },
                    )
                    Spacer(Modifier.weight(1f))
                }
                TextButton(onClick = { deleting = selected }, enabled = !state.busy) {
                    Text(stringResource(R.string.notes_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (state.editorOpen && state.editingNote == null) {
        NoteEditorDialog(
            note = null,
            busy = state.busy,
            onSave = viewModel::save,
            onDismiss = viewModel::closeEditor,
        )
    }

    if (showGenerate) {
        GenerateNotesDialog(
            onGenerate = { count ->
                showGenerate = false
                viewModel.generate(count)
            },
            onDismiss = { showGenerate = false },
        )
    }

    deleting?.let { note ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.notes_delete_note)) },
            text = { Text(stringResource(R.string.notes_delete_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    viewModel.delete(note)
                }) {
                    Text(stringResource(R.string.notes_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun NoteListCard(note: LocalNote, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    note.title.ifBlank { note.body.lineSequence().firstOrNull().orEmpty() },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (note.pinned) {
                    Spacer(Modifier.width(8.dp))
                    Text("•", style = MaterialTheme.typography.titleMedium)
                }
            }
            if (note.title.isNotBlank()) {
                Text(
                    note.body,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SecretAwareBody(text: String, onTap: (Int) -> Unit) {
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        onTextLayout = { layout = it },
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(text) {
                detectTapGestures { position ->
                    val result = layout ?: return@detectTapGestures
                    if (text.isNotEmpty()) {
                        onTap(result.getOffsetForPosition(position).coerceIn(0, text.lastIndex))
                    }
                }
            },
    )
}

@Composable
internal fun NoteEditorDialog(
    note: LocalNote?,
    busy: Boolean,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var body by remember(note?.id) { mutableStateOf(note?.body.orEmpty()) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(if (note == null) R.string.notes_add else R.string.notes_edit_note)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= NotesPolicy.MAX_TITLE_LENGTH) title = it },
                    enabled = !busy,
                    label = { Text(stringResource(R.string.notes_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { if (it.length <= NotesPolicy.MAX_BODY_LENGTH) body = it },
                    enabled = !busy,
                    label = { Text(stringResource(R.string.notes_body_label)) },
                    minLines = 5,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = NotesPolicy.isValid(title, body) && !busy,
                onClick = { onSave(title, body) },
            ) {
                Text(stringResource(R.string.notes_save))
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
internal fun GenerateNotesDialog(onGenerate: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notes_generate_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.notes_generate_description))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 5, 7, 10).forEach { count ->
                        TextButton(onClick = { onGenerate(count) }) { Text(count.toString()) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
