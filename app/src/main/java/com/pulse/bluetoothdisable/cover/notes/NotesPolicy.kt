package com.pulse.bluetoothdisable.cover.notes

internal object NotesPolicy {
    const val MAX_TITLE_LENGTH = 120
    const val MAX_BODY_LENGTH = 10_000
    const val MAX_SECRET_LENGTH = 80

    fun normalizeTitle(value: String): String = value.trim()
    fun normalizeBody(value: String): String = value.trim()

    fun isValid(title: String, body: String): Boolean =
        normalizeTitle(title).length <= MAX_TITLE_LENGTH &&
            normalizeBody(body).isNotEmpty() &&
            normalizeBody(body).length <= MAX_BODY_LENGTH

    fun isValidSecretRange(body: String, start: Int, end: Int): Boolean =
        start >= 0 && end > start && end <= body.length &&
            end - start <= MAX_SECRET_LENGTH &&
            body.substring(start, end).isNotBlank()
}
