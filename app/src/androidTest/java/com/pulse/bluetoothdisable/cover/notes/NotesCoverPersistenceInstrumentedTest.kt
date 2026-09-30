package com.pulse.bluetoothdisable.cover.notes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pulse.bluetoothdisable.cover.CoverMode
import com.pulse.bluetoothdisable.cover.CoverModeManager
import com.pulse.bluetoothdisable.launcher.LauncherStyle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotesCoverPersistenceInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository = LocalNotesRepository(context)
    private val access = NotesAccessManager(context)
    private val modes = CoverModeManager(context)

    @Before fun before() = clean()
    @After fun after() = clean()

    @Test fun notesPersistAcrossCoverModesButAccessRuleDoesNot() {
        val note = repository.save("Plans", "buy milk tomorrow")
        modes.activateNotes(note.id, 4, 8, note.body)

        assertEquals(CoverMode.NOTES, modes.activeMode())
        assertTrue(access.hasRule())
        assertTrue(access.matchesTap(note.id, note.body, 5))
        assertFalse(access.matchesTap(note.id, note.body, 0))

        modes.deactivateToLauncher(LauncherStyle.DEFAULT)
        assertEquals(CoverMode.DEFAULT, modes.activeMode())
        assertFalse(access.hasRule())
        assertEquals("buy milk tomorrow", repository.notes().single().body)

        modes.activateCalculator("12345")
        modes.deactivateToLauncher(LauncherStyle.DEFAULT)
        assertEquals("buy milk tomorrow", repository.notes().single().body)
    }

    private fun clean() {
        runCatching { modes.resetToDefault() }
        runCatching { access.clear() }
        runCatching { repository.notes().forEach { repository.delete(it.id) } }
    }
}
