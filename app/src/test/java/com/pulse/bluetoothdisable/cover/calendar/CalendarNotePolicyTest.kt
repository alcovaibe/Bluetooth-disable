package com.pulse.bluetoothdisable.cover.calendar

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarNotePolicyTest {
    private val date = LocalDate.of(2026, 10, 3)

    @Test fun trimsEdgesKeepsInternalLinesAndCountsUtf16Units() {
        assertEquals("one\ntwo", CalendarNotePolicy.normalize("  one\ntwo\n "))
        assertNull(CalendarNotePolicy.validate(date, "a".repeat(120), emptyList()))
        assertEquals(
            CalendarNotePolicy.Violation.TOO_LONG,
            CalendarNotePolicy.validate(date, "a".repeat(121), emptyList()),
        )
        assertEquals(2, "😀".length)
        assertNull(CalendarNotePolicy.validate(date, "😀".repeat(60), emptyList()))
        assertEquals(
            CalendarNotePolicy.Violation.TOO_LONG,
            CalendarNotePolicy.validate(date, "😀".repeat(61), emptyList()),
        )
    }

    @Test fun rejectsBlankDuplicateAndTwentySixthNote() {
        assertEquals(
            CalendarNotePolicy.Violation.EMPTY,
            CalendarNotePolicy.validate(date, " \n ", emptyList()),
        )
        val existing = note("1", date, "Same text", 1L, 1L)
        assertEquals(
            CalendarNotePolicy.Violation.DUPLICATE,
            CalendarNotePolicy.validate(date, "  Same text  ", listOf(existing)),
        )
        assertNull(CalendarNotePolicy.validate(date, "same text", listOf(existing)))

        val fullDate = (1..25).map { index ->
            note(index.toString(), date, "note $index", index.toLong(), index.toLong())
        }
        assertEquals(
            CalendarNotePolicy.Violation.DATE_LIMIT,
            CalendarNotePolicy.validate(date, "note 26", fullDate),
        )
        assertNull(
            CalendarNotePolicy.validate(
                date,
                "updated note",
                fullDate,
                editingId = fullDate.first().id,
            ),
        )
    }

    @Test fun ordersByMostRecentlyUpdatedThenCreated() {
        val oldest = note("a", date, "a", created = 1L, updated = 1L)
        val newer = note("b", date, "b", created = 2L, updated = 2L)
        val edited = note("c", date, "c", created = 0L, updated = 3L)
        assertEquals(listOf("c", "b", "a"), CalendarNotePolicy.sorted(listOf(oldest, edited, newer)).map { it.id })
    }

    private fun note(
        id: String,
        date: LocalDate,
        text: String,
        created: Long,
        updated: Long,
    ) = CalendarNote(id, date, text, created, updated)
}
