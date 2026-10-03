package com.pulse.bluetoothdisable.cover.calendar

import java.time.LocalDate

/** Validation and ordering rules shared by the Calendar UI and persistent repository. */
object CalendarNotePolicy {
    const val MAX_TEXT_LENGTH = 120
    const val MAX_NOTES_PER_DATE = 25

    enum class Violation {
        EMPTY,
        TOO_LONG,
        DUPLICATE,
        DATE_LIMIT,
    }

    fun normalize(text: String): String = text.trim()

    fun validate(
        date: LocalDate,
        text: String,
        notes: List<CalendarNote>,
        editingId: String? = null,
    ): Violation? {
        val normalized = normalize(text)
        if (normalized.isEmpty()) return Violation.EMPTY
        if (normalized.length > MAX_TEXT_LENGTH) return Violation.TOO_LONG

        val otherNotesOnDate = notes.filter { note ->
            note.date == date && note.id != editingId
        }
        if (otherNotesOnDate.any { normalize(it.text) == normalized }) {
            return Violation.DUPLICATE
        }
        if (editingId == null && otherNotesOnDate.size >= MAX_NOTES_PER_DATE) {
            return Violation.DATE_LIMIT
        }
        return null
    }

    fun sorted(notes: List<CalendarNote>): List<CalendarNote> =
        notes.sortedWith(
            compareByDescending<CalendarNote> { it.updatedAt }
                .thenByDescending { it.createdAt }
                .thenBy { it.id },
        )
}

class CalendarNoteValidationException(
    val violation: CalendarNotePolicy.Violation,
) : IllegalArgumentException(violation.name)
