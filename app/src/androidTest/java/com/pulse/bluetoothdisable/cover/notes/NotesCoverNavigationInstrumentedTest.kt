package com.pulse.bluetoothdisable.cover.notes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pulse.bluetoothdisable.cover.CoverMode
import com.pulse.bluetoothdisable.cover.CoverModeManager
import com.pulse.bluetoothdisable.cover.CoverRecoveryManager
import com.pulse.bluetoothdisable.launcher.LauncherIconController
import com.pulse.bluetoothdisable.launcher.LauncherStyle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotesCoverNavigationInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository = LocalNotesRepository(context)
    private val manager = CoverModeManager(context)

    @Before
    fun before() {
        runCatching { manager.resetToDefault() }
        repository.clear()
    }

    @After
    fun after() {
        runCatching { manager.resetToDefault() }
        repository.clear()
    }

    @Test
    fun emergencyResetKeepsNotesAndRestoresDefaultLauncher() {
        val note = repository.save("Access", "hidden fragment here")
        manager.activateNotes(note.id, 0, 6, note.body)
        assertEquals(CoverMode.NOTES, manager.activeMode())

        val recovery = CoverRecoveryManager(
            activeMode = manager::activeMode,
            resetCover = manager::resetNotesCover,
            modeToRecover = CoverMode.NOTES,
        )
        val attempt = recovery.begin()!!
        recovery.authenticationSucceeded(attempt)
        assertTrue(recovery.confirmReset())

        assertEquals(CoverMode.DEFAULT, manager.activeMode())
        assertTrue(LauncherIconController(context).isExclusivelyEnabled(LauncherStyle.DEFAULT))
        assertEquals(note.body, repository.notes().single().body)
        assertFalse(NotesAccessManager(context).hasRule())
    }
}
