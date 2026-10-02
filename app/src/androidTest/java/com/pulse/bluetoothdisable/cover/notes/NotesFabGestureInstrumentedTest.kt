package com.pulse.bluetoothdisable.cover.notes

import android.app.Application
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pulse.bluetoothdisable.R
import com.pulse.bluetoothdisable.ui.theme.BluetoothDisableTheme
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotesFabGestureInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val repository = LocalNotesRepository(app)
    private lateinit var viewModel: NotesViewModel

    @Before
    fun before() {
        repository.clear()
        compose.runOnUiThread { viewModel = NotesViewModel(app) }
    }

    @After
    fun after() {
        repository.clear()
    }

    private fun show(onRecovery: () -> Unit = {}) {
        compose.setContent {
            BluetoothDisableTheme {
                NotesScreen(
                    viewModel = viewModel,
                    recoveryEnabled = true,
                    onRecoveryHold = onRecovery,
                    onUnlock = {},
                )
            }
        }
        compose.waitUntil(3_000) { !viewModel.uiState.loading }
    }

    @Test
    fun shortTapOpensNewNoteEditorAndDoesNotRunRecovery() {
        val recoveries = AtomicInteger()
        show { recoveries.incrementAndGet() }

        compose.onNodeWithContentDescription(app.getString(R.string.notes_add)).performClick()
        compose.waitForIdle()

        assertEquals(0, recoveries.get())
        compose.onNodeWithContentDescription(app.getString(R.string.notes_back)).assertExists()
        assertEquals(true, viewModel.uiState.editorOpen)
        assertEquals(true, viewModel.uiState.draftIsNew)
    }

    @Test
    fun threeSecondHoldRunsRecoveryOnceWithoutOpeningEditor() {
        val recoveries = AtomicInteger()
        show { recoveries.incrementAndGet() }
        compose.mainClock.autoAdvance = false
        val fab = compose.onNodeWithContentDescription(app.getString(R.string.notes_add))

        fab.performTouchInput { down(center) }
        compose.mainClock.advanceTimeByFrame()
        SystemClock.sleep(3_150)
        compose.mainClock.advanceTimeBy(3_150)
        compose.waitUntil(2_000) { recoveries.get() == 1 }
        fab.performTouchInput { up() }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()

        assertEquals(1, recoveries.get())
        assertEquals(false, viewModel.uiState.editorOpen)
        compose.onNodeWithContentDescription(app.getString(R.string.notes_new_checklist)).assertExists()
    }

    @Test
    fun earlyReleaseRemainsOrdinaryTap() {
        val recoveries = AtomicInteger()
        show { recoveries.incrementAndGet() }
        compose.mainClock.autoAdvance = false
        val fab = compose.onNodeWithContentDescription(app.getString(R.string.notes_add))

        fab.performTouchInput { down(center) }
        compose.mainClock.advanceTimeByFrame()
        SystemClock.sleep(500)
        compose.mainClock.advanceTimeBy(500)
        fab.performTouchInput { up() }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()

        assertEquals(0, recoveries.get())
        assertEquals(true, viewModel.uiState.editorOpen)
    }
}
