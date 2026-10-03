package com.pulse.bluetoothdisable.cover.calendar

import java.security.MessageDigest
import java.time.LocalDate
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarAccessPolicyTest {
    private val date = LocalDate.of(2012, 12, 12)

    private fun sign(date: LocalDate, text: String): ByteArray = Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(ByteArray(32) { it.toByte() }, "HmacSHA256"))
        doFinal(CalendarAccessPolicy.payload(date, text))
    }

    @Test fun dateAndExactTextAreRequired() {
        val expected = sign(date, "открой меня")
        assertTrue(MessageDigest.isEqual(expected, sign(date, "открой меня")))
        assertTrue(MessageDigest.isEqual(expected, sign(date, " открой меня ")))
        assertFalse(MessageDigest.isEqual(expected, sign(date.plusDays(1), "открой меня")))
        assertFalse(MessageDigest.isEqual(expected, sign(date, "привет")))
        assertFalse(MessageDigest.isEqual(expected, sign(date, "Открой меня")))
        assertFalse(MessageDigest.isEqual(expected, sign(date, "открой  меня")))
    }

    @Test fun trimmedLengthUsesUtf16UnitsAndAlwaysFitsInsideANote() {
        assertFalse(CalendarAccessPolicy.isValid("   "))
        assertFalse(CalendarAccessPolicy.isValid("ab"))
        assertTrue(CalendarAccessPolicy.isValid(" abc "))
        assertTrue(CalendarAccessPolicy.isValid("a".repeat(100)))
        assertFalse(CalendarAccessPolicy.isValid("a".repeat(101)))
        assertFalse(CalendarAccessPolicy.isValid("😀"))
        assertTrue(CalendarAccessPolicy.isValid("😀😀"))
        assertTrue(CalendarAccessPolicy.isValid("😀".repeat(50)))
        assertFalse(CalendarAccessPolicy.isValid("😀".repeat(51)))
    }
}
