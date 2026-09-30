package com.pulse.bluetoothdisable.cover.calendar

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarAccessManagerInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = CalendarAccessManager(context)
    private val date = LocalDate.of(2012, 12, 12)

    @Before fun before() = manager.clear()
    @After fun after() = manager.clear()

    @Test fun exactTrimmedDateAndTextRuleUsesDeviceKey() {
        manager.setAccessRule(date, "открой меня")
        assertTrue(manager.hasRule())
        assertEquals(date, manager.secretDate())
        assertTrue(manager.verify(date, "открой меня"))
        assertTrue(manager.verify(date, " открой меня "))
        assertFalse(manager.verify(date.plusDays(1), "открой меня"))
        assertFalse(manager.verify(date, "привет"))
        assertFalse(manager.verify(date, "Открой меня"))
        val values = context.getSharedPreferences(CalendarAccessManager.PREFERENCES_NAME, Context.MODE_PRIVATE).all
        assertFalse(values.values.any { it.toString().contains("открой меня") })
        manager.clear()
        assertFalse(manager.hasRule())
        assertNull(manager.secretDate())
        assertFalse(manager.verify(date, "открой меня"))
    }

    @Test fun copiedVerifierWithoutKeystoreKeyCannotUnlock() {
        manager.setAccessRule(date, "открой меня")
        val prefs = context.getSharedPreferences(CalendarAccessManager.PREFERENCES_NAME, Context.MODE_PRIVATE)
        val copied = prefs.all
        manager.clear()
        val editor = prefs.edit()
        copied.forEach { (key, value) -> editor.putString(key, value as String) }
        assertTrue(editor.commit())
        assertFalse(manager.hasRule())
        assertFalse(manager.verify(date, "открой меня"))
    }
}
