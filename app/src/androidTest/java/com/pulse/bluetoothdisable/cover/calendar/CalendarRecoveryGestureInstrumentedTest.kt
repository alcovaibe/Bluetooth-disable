package com.pulse.bluetoothdisable.cover.calendar

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pulse.bluetoothdisable.cover.CoverModeManager
import com.pulse.bluetoothdisable.ui.theme.BluetoothDisableTheme
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarRecoveryGestureInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val calls = AtomicInteger()
    private val enabled = mutableStateOf(true)
    private lateinit var model: CalendarViewModel

    @Before fun before() { CoverModeManager(app).resetToDefault() }
    @After fun after() { CoverModeManager(app).resetToDefault() }

    private fun show() {
        compose.runOnUiThread { model = CalendarViewModel(app) }
        compose.setContent {
            BluetoothDisableTheme {
                CalendarScreen(model, enabled.value, { calls.incrementAndGet() }, {})
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
    }

    private fun advanceHold(millis: Long) {
        compose.mainClock.advanceTimeByFrame()
        SystemClock.sleep(millis)
        compose.mainClock.advanceTimeBy(millis)
    }

    @Test fun calendarAndTodayTextEachTriggerOnceAfterSevenSecondsWithFingerDrift() {
        show()
        for ((index, word) in listOf("Calendar", "Today").withIndex()) {
            val node = compose.onNodeWithText(word, useUnmergedTree = true)
            node.performTouchInput {
                down(center)
                moveBy(Offset(10f, 6f))
                moveBy(Offset(-4f, 3f))
            }
            advanceHold(7_200)
            compose.waitUntil(2_000) { calls.get() == index + 1 }
            compose.mainClock.advanceTimeBy(3_000)
            assertEquals(index + 1, calls.get())
            node.performTouchInput { up() }
        }
    }

    @Test fun earlyReleaseSeparateHoldsAndDisabledTextDoNotTriggerAndTodayStillWorks() {
        show()
        for (word in listOf("Calendar", "Today")) {
            val node = compose.onNodeWithText(word, useUnmergedTree = true)
            node.performTouchInput { down(center) }
            advanceHold(4_000)
            node.performTouchInput { up() }
        }
        assertEquals(0, calls.get())
        val title = compose.onNodeWithText("Calendar", useUnmergedTree = true)
        title.performTouchInput { down(center) }
        compose.runOnIdle { enabled.value = false }
        advanceHold(7_200)
        title.performTouchInput { up() }
        assertEquals(0, calls.get())
        compose.runOnIdle { model.select(LocalDate.of(2024, 2, 29)) }
        compose.mainClock.autoAdvance = true
        compose.onNodeWithText("Today").performClick()
        compose.runOnIdle { assertEquals(LocalDate.now(), model.uiState.selectedDate) }
        assertEquals(0, calls.get())
    }

    @Test fun blankToolbarAndDateLabelDoNotTrigger() {
        show()
        val toolbar = compose.onNodeWithTag("calendar_toolbar")
        toolbar.performTouchInput { down(Offset(width / 2f, center.y)) }
        advanceHold(7_200)
        toolbar.performTouchInput { up() }
        val date = compose.onNodeWithText(CalendarDates.formatDate(LocalDate.now(), Locale.US))
        date.performTouchInput { down(center) }
        advanceHold(7_200)
        date.performTouchInput { up() }
        assertEquals(0, calls.get())
    }
}
