package com.pulse.bluetoothdisable.cover.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object CalendarDates {
    val YEAR_RANGE = 1..9999

    fun monthCells(month: YearMonth): List<LocalDate?> {
        val offset = month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value
        val cells = List<LocalDate?>(offset) { null } +
            (1..month.lengthOfMonth()).map(month::atDay)
        return cells + List((7 - cells.size % 7) % 7) { null }
    }

    fun fromPickerMillis(millis: Long): LocalDate =
        java.time.Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    fun toPickerMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun formatDate(date: LocalDate, locale: Locale): String = date.format(
        DateTimeFormatter.ofPattern(
            if (locale.language == "ru") "d MMMM uuuu" else "MMMM d, uuuu", locale,
        ),
    )

    fun formatMonth(month: YearMonth, locale: Locale): String =
        month.format(DateTimeFormatter.ofPattern("LLLL uuuu", locale))
            .replaceFirstChar { it.titlecase(locale) }
}
