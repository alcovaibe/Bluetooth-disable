package com.pulse.bluetoothdisable.cover.calendar

import java.time.DateTimeException
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class CalendarDatesTest {
    @Test fun leapAndOrdinaryFebruary() {
        assertEquals(29, CalendarDates.monthCells(YearMonth.of(2024, 2)).filterNotNull().size)
        assertEquals(28, CalendarDates.monthCells(YearMonth.of(2023, 2)).filterNotNull().size)
        assertEquals(LocalDate.of(2024, 2, 29), LocalDate.parse("2024-02-29"))
        assertThrows(DateTimeException::class.java) { LocalDate.parse("2023-02-29") }
        assertEquals(28, YearMonth.of(1900, 2).lengthOfMonth())
        assertEquals(29, YearMonth.of(2000, 2).lengthOfMonth())
    }

    @Test fun everyMonthContainsExactlyItsDatesInMondayFirstWeeks() {
        for (year in listOf(1, 1900, 2000, 2023, 2024, 2026, 9999)) {
            for (monthNumber in 1..12) {
                val month = YearMonth.of(year, monthNumber)
                val cells = CalendarDates.monthCells(month)
                assertEquals(0, cells.size % 7)
                assertEquals((1..month.lengthOfMonth()).map(month::atDay), cells.filterNotNull())
                assertEquals(month.atDay(1).dayOfWeek.value - 1, cells.indexOf(month.atDay(1)))
            }
        }
    }

    @Test fun yearBoundariesAndIsoStorage() {
        assertEquals(YearMonth.of(2027, 1), YearMonth.of(2026, 12).plusMonths(1))
        assertEquals(YearMonth.of(2025, 12), YearMonth.of(2026, 1).minusMonths(1))
        val date = LocalDate.of(2012, 12, 12)
        assertEquals("2012-12-12", date.toString())
        assertEquals(date, LocalDate.parse(date.toString()))
    }

    @Test fun datePickerUtcConversionRoundTrips() {
        for (date in listOf(LocalDate.of(1, 1, 1), LocalDate.of(2012, 12, 12),
            LocalDate.of(2024, 2, 29), LocalDate.of(9999, 12, 31))) {
            assertEquals(date, CalendarDates.fromPickerMillis(CalendarDates.toPickerMillis(date)))
        }
    }

    @Test fun localizedDateRendering() {
        val date = LocalDate.of(2026, 9, 30)
        assertEquals("30 сентября 2026", CalendarDates.formatDate(date, Locale.forLanguageTag("ru")))
        assertEquals("September 30, 2026", CalendarDates.formatDate(date, Locale.ENGLISH))
    }
}
