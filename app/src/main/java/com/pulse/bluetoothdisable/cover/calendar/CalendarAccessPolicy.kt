package com.pulse.bluetoothdisable.cover.calendar

import java.time.LocalDate

object CalendarAccessPolicy {
    const val MIN_LENGTH = 3
    const val MAX_LENGTH = 100

    fun normalize(text: String): String = text.trim()

    fun isValid(text: String): Boolean {
        val normalized = normalize(text)
        return normalized.codePointCount(0, normalized.length) in MIN_LENGTH..MAX_LENGTH
    }

    // Bind the date to the verifier too. Changing a stored date cannot move the entry rule.
    fun payload(date: LocalDate, text: String): ByteArray =
        "calendar-access-v1\u0000$date\u0000${normalize(text)}".toByteArray(Charsets.UTF_8)
}
