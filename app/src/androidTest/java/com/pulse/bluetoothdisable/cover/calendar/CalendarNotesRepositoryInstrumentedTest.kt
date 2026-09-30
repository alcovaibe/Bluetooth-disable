package com.pulse.bluetoothdisable.cover.calendar

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarNotesRepositoryInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository = LocalCalendarNotesRepository(context)
    private val date = LocalDate.of(2024, 2, 29)

    @Before fun before() = repository.clear()
    @After fun after() = repository.clear()

    @Test fun multipleNotesPersistOnTheirOwnDateAndCanBeEditedAndDeleted() {
        val first = repository.save(date, "Купить хлеб")
        val second = repository.save(date, "Позвонить")
        val other = repository.save(date.plusDays(1), "Документ")
        val reopened = LocalCalendarNotesRepository(context)
        assertEquals(2, reopened.notes().count { it.date == date })
        assertEquals(date.plusDays(1), reopened.notes().single { it.id == other.id }.date)
        val edited = reopened.save(date, "Купить молоко", first.id)
        assertEquals(first.id, edited.id)
        assertEquals(first.createdAt, edited.createdAt)
        assertEquals("Купить молоко", reopened.notes().single { it.id == first.id }.text)
        reopened.delete(second.id)
        assertFalse(repository.notes().any { it.id == second.id })
        val json = File(context.filesDir, LocalCalendarNotesRepository.FILE_NAME).readText()
        assertFalse(json.contains("Купить молоко"))
        assertFalse(json.contains("2024-02-29"))
        assertEquals("2024-02-29", reopened.notes().first().date.toString())
        repository.clear()
        assertTrue(repository.notes().isEmpty())
    }

    @Test fun encryptedFileCannotBeReadAfterDeviceKeyIsLost() {
        repository.save(date, "открой меня")
        val file = File(context.filesDir, LocalCalendarNotesRepository.FILE_NAME)
        assertFalse(file.readText().contains("открой меня"))
        CalendarNotesCipher.clearKey()
        assertThrows(IllegalStateException::class.java) { repository.notes() }
    }

    @Test fun blankNoteIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { repository.save(date, "  ") }
        assertTrue(repository.notes().isEmpty())
    }
}
