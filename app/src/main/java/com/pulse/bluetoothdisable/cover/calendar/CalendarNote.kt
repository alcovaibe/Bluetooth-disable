package com.pulse.bluetoothdisable.cover.calendar

import java.time.LocalDate

data class CalendarNote(
    val id: String,
    val date: LocalDate,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long,
)

interface CalendarNotesRepository {
    fun notes(): List<CalendarNote>
    fun save(date: LocalDate, text: String, id: String? = null): CalendarNote
    fun delete(id: String)
    fun clear()
}
