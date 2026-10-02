package com.pulse.bluetoothdisable.cover.notes

internal object NotesPolicy {
    const val MAX_TITLE_LENGTH = 120
    const val MAX_BODY_LENGTH = 20_000
    const val MAX_SECRET_LENGTH = 80
    const val MAX_CHECKLIST_ITEM_LENGTH = 500
    const val MAX_IMAGE_BYTES = 30L * 1024L * 1024L

    fun normalizeTitle(value: String): String = value.trim()

    /** Preserve body whitespace exactly; trimming user text caused silent data loss. */
    fun normalizeBody(value: String): String = value

    fun isValid(title: String, body: String): Boolean =
        normalizeTitle(title).length <= MAX_TITLE_LENGTH &&
            body.length <= MAX_BODY_LENGTH &&
            (normalizeTitle(title).isNotEmpty() || body.isNotBlank())

    fun isPersistable(note: LocalNote): Boolean {
        if (normalizeTitle(note.title).length > MAX_TITLE_LENGTH || note.body.length > MAX_BODY_LENGTH) return false
        if (note.checklist.any { it.text.length > MAX_CHECKLIST_ITEM_LENGTH }) return false
        return normalizeTitle(note.title).isNotEmpty() ||
            note.body.isNotBlank() ||
            note.images.isNotEmpty() ||
            note.checklist.any { it.text.isNotBlank() }
    }

    fun normalizeStyles(body: String, styles: List<NoteTextStyle>): List<NoteTextStyle> =
        styles.mapNotNull { style ->
            val start = style.start.coerceIn(0, body.length)
            val end = style.end.coerceIn(start, body.length)
            if (start == end) null else style.copy(start = start, end = end)
        }

    fun isValidSecretRange(body: String, start: Int, end: Int): Boolean =
        start >= 0 && end > start && end <= body.length &&
            end - start <= MAX_SECRET_LENGTH &&
            body.substring(start, end).isNotBlank()
}
