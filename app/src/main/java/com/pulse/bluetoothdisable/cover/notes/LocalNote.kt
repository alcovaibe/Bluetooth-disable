package com.pulse.bluetoothdisable.cover.notes

enum class NoteType { TEXT, CHECKLIST }

data class NoteTextStyle(
    val start: Int,
    val end: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikeThrough: Boolean = false,
)

data class NoteImage(
    val id: String,
    val fileName: String,
    val offset: Int,
)

data class ChecklistItem(
    val id: String,
    val text: String,
    val checked: Boolean,
)

data class LocalNote(
    val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val favorite: Boolean,
    val type: NoteType = NoteType.TEXT,
    val styles: List<NoteTextStyle> = emptyList(),
    val images: List<NoteImage> = emptyList(),
    val checklist: List<ChecklistItem> = emptyList(),
) {
    /** Backward-compatible alias used by setup/recovery code migrated from the old pin flag. */
    val pinned: Boolean get() = favorite
}
