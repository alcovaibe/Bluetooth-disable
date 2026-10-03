package com.pulse.bluetoothdisable.cover.calendar

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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

    @Test fun notesPersistTrimSortEditWithoutChangingDateAndDelete() {
        val first = repository.save(date, "  Купить хлеб\n")
        val second = repository.save(date, "Позвонить")
        val other = repository.save(date.plusDays(1), "Документ")
        val reopened = LocalCalendarNotesRepository(context)

        assertEquals(2, reopened.notes().count { it.date == date })
        assertEquals(date.plusDays(1), reopened.notes().single { it.id == other.id }.date)
        assertEquals("Купить хлеб", reopened.notes().single { it.id == first.id }.text)
        assertEquals(second.id, reopened.notes().first { it.date == date }.id)

        val edited = reopened.save(date, "Купить молоко", first.id)
        assertEquals(first.id, edited.id)
        assertEquals(first.createdAt, edited.createdAt)
        assertEquals(date, edited.date)
        assertEquals(first.id, reopened.notes().first { it.date == date }.id)
        assertThrows(IllegalArgumentException::class.java) {
            reopened.save(date.plusDays(2), "Нельзя перенести", first.id)
        }

        reopened.delete(second.id)
        assertFalse(repository.notes().any { it.id == second.id })
        val encrypted = File(context.filesDir, LocalCalendarNotesRepository.FILE_NAME).readText()
        assertFalse(encrypted.contains("Купить молоко"))
        assertFalse(encrypted.contains("2024-02-29"))
    }

    @Test fun validationRejectsBlankTooLongDuplicateAndTwentySixthNote() {
        var error = assertThrows(CalendarNoteValidationException::class.java) {
            repository.save(date, "  \n ")
        }
        assertEquals(CalendarNotePolicy.Violation.EMPTY, error.violation)

        error = assertThrows(CalendarNoteValidationException::class.java) {
            repository.save(date, "x".repeat(121))
        }
        assertEquals(CalendarNotePolicy.Violation.TOO_LONG, error.violation)

        repository.save(date, "Same")
        error = assertThrows(CalendarNoteValidationException::class.java) {
            repository.save(date, "  Same  ")
        }
        assertEquals(CalendarNotePolicy.Violation.DUPLICATE, error.violation)
        repository.save(date, "same")

        repository.clear()
        repeat(CalendarNotePolicy.MAX_NOTES_PER_DATE) { index ->
            repository.save(date, "note-$index")
        }
        error = assertThrows(CalendarNoteValidationException::class.java) {
            repository.save(date, "one-too-many")
        }
        assertEquals(CalendarNotePolicy.Violation.DATE_LIMIT, error.violation)
        assertEquals(CalendarNotePolicy.MAX_NOTES_PER_DATE, repository.notes().count { it.date == date })
        repository.save(date.plusDays(1), "allowed on another date")
    }

    @Test fun encryptedFileCannotBeReadAfterDeviceKeyIsLost() {
        repository.save(date, "открой меня")
        val file = File(context.filesDir, LocalCalendarNotesRepository.FILE_NAME)
        assertFalse(file.readText().contains("открой меня"))
        CalendarNotesCipher.clearKey()
        assertThrows(IllegalStateException::class.java) { repository.notes() }
    }

    @Test fun draftsAreEncryptedPersistByDateAndAreClearedWithCalendarData() {
        val drafts = CalendarDraftStore(context)
        drafts.put(date, text = "  draft\ntext  ")
        assertEquals("  draft\ntext  ", CalendarDraftStore(context).text(date))

        val draftFile = File(context.filesDir, CalendarDraftStore.FILE_NAME)
        assertTrue(draftFile.exists())
        assertFalse(draftFile.readText().contains("draft"))
        assertNull(CalendarDraftStore(context).text(date.plusDays(1)))

        val note = repository.save(date, "saved")
        drafts.put(date, note.id, "edited draft")
        assertEquals("edited draft", CalendarDraftStore(context).text(date, note.id))

        repository.clear()
        assertFalse(draftFile.exists())
        assertNull(CalendarDraftStore(context).text(date))
        assertTrue(repository.notes().isEmpty())
    }
}
