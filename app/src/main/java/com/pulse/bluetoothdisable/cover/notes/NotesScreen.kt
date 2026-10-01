package com.pulse.bluetoothdisable.cover.notes

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulse.bluetoothdisable.R
import com.pulse.bluetoothdisable.cover.coverRecoveryHold
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val GENERATE_HOLD_MILLIS = 3_000L

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
    var showGenerateMenu by remember { mutableStateOf(false) }
    var showNoteActions by remember(selected?.id) { mutableStateOf(false) }

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
                )
            },
            floatingActionButton = {
                Box {
                    FloatingActionButton(
                        onClick = {},
                        modifier = Modifier.pointerInput(state.busy) {
                            detectTapGestures(
                                onPress = {
                                    if (state.busy) {
                                        tryAwaitRelease()
                                    } else {
                                        var holdTriggered = false
                                        coroutineScope {
                                            val holdJob = launch {
                                                delay(GENERATE_HOLD_MILLIS)
                                                holdTriggered = true
                                                showGenerateMenu = true
                                            }
                                            val released = tryAwaitRelease()
                                            holdJob.cancel()
                                            if (released && !holdTriggered) {
                                                viewModel.add()
                                            }
                                        }
                                    }
                                },
                            )
                        },
                    ) {
                        Text("+", style = MaterialTheme.typography.headlineMedium)
                    }
                    DropdownMenu(
                        expanded = showGenerateMenu,
                        onDismissRequest = { showGenerateMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.notes_generate)) },
                            onClick = {
                                showGenerateMenu = false
                                showGenerate = true
                            },
                        )
                    }
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
                        IconButton(
                            onClick = {
                                if (inlineEditing) viewModel.closeEditor() else viewModel.closeNote()
                            },
                            enabled = !state.busy,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.notes_back),
                            )
                        }
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
                        }
                    },
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    if (inlineEditing) {
                        val titleContainer = MaterialTheme.colorScheme.surfaceContainerLow
                        TextField(
                            value = inlineTitle,
                            onValueChange = {
                                if (it.length <= NotesPolicy.MAX_TITLE_LENGTH) inlineTitle = it
                            },
                            enabled = !state.busy,
                            placeholder = { Text(stringResource(R.string.notes_title_label)) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = titleContainer,
                                unfocusedContainerColor = titleContainer,
                                disabledContainerColor = titleContainer,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextField(
                            value = inlineBody,
                            onValueChange = {
                                if (it.length <= NotesPolicy.MAX_BODY_LENGTH) inlineBody = it
                            },
                            enabled = !state.busy,
                            placeholder = { Text(stringResource(R.string.notes_body_label)) },
                            shape = RoundedCornerShape(0.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
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
                        Spacer(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .pointerInput(selected.id, state.busy) {
                                    detectTapGestures {
                                        if (!state.busy) {
                                            showNoteActions = !showNoteActions
                                        }
                                    }
                                },
                        )
                    }
                }

                if (showNoteActions && !inlineEditing) {
                    NotesActionDock(
                        pinned = selected.pinned,
                        onFavorite = {
                            showNoteActions = false
                            viewModel.setPinned(selected, !selected.pinned)
                        },
                        onEdit = {
                            showNoteActions = false
                            viewModel.edit(selected)
                        },
                        onDelete = {
                            showNoteActions = false
                            deleting = selected
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 28.dp, vertical = 16.dp)
                            .navigationBarsPadding(),
                    )
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
private fun NotesActionDock(
    pinned: Boolean,
    onFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NoteActionItem(
                icon = if (pinned) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                label = stringResource(R.string.notes_pin),
                tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                onClick = onFavorite,
                modifier = Modifier.weight(1f),
            )
            NoteActionItem(
                icon = Icons.Rounded.Edit,
                label = stringResource(R.string.notes_edit),
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = onEdit,
                modifier = Modifier.weight(1f),
            )
            NoteActionItem(
                icon = Icons.Rounded.Delete,
                label = stringResource(R.string.notes_delete),
                tint = MaterialTheme.colorScheme.error,
                onClick = onDelete,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NoteActionItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
