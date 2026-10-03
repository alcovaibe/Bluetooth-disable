package com.pulse.bluetoothdisable.cover.calculator

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pulse.bluetoothdisable.R
import com.pulse.bluetoothdisable.ui.theme.BluetoothDisableTheme
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryRecoveryGestureInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val calls = AtomicInteger()
    private val enabled = mutableStateOf(true)
    private val history = mutableStateOf(listOf(CalculatorHistoryEntry("1+1", "2")))

    private fun showHistory() {
        compose.setContent {
            BluetoothDisableTheme {
                CalculatorHistoryDrawer(
                    history.value,
                    false,
                    { history.value = emptyList() },
                    enabled.value,
                    { calls.incrementAndGet() },
                )
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
    }

    private fun title() = compose.onNodeWithText(context.getString(R.string.calculator_history))

    private fun advanceHold(millis: Long) {
        compose.mainClock.advanceTimeByFrame()
        SystemClock.sleep(millis)
        compose.mainClock.advanceTimeBy(millis)
    }

    @Test fun blankHeaderAreaAndPaddingTriggerOncePerHold() {
        showHistory()
        val header = compose.onNodeWithTag("calculator_history_header")
        // Both points are outside the History text: right-side blank space and
        // the lower-left padding of the colored header rectangle.
        repeat(2) { index ->
            header.performTouchInput {
                down(if (index == 0) Offset(width - 4f, center.y) else Offset(4f, height - 4f))
            }
            advanceHold(3_200)
            compose.waitUntil(2_000) { calls.get() == index + 1 }
            compose.mainClock.advanceTimeBy(1_000)
            assertEquals(index + 1, calls.get())
            header.performTouchInput { up() }
        }
    }

    @Test fun historyRowsAndClearButtonAreOutsideRecoveryArea() {
        showHistory()
        val row = compose.onNodeWithText("1+1 = 2")
        row.performTouchInput { down(center) }
        advanceHold(3_200)
        assertEquals(0, calls.get())
        row.performTouchInput { up() }

        val clear = compose.onNodeWithTag("calculator_history_clear")
        clear.performTouchInput { down(center) }
        advanceHold(3_200)
        assertEquals(0, calls.get())
        clear.performTouchInput { up() }
        assertEquals(0, calls.get())
    }

    @Test fun threeSecondHoldFiresOnceEvenWhenFingerRemainsDown() {
        showHistory()
        title().performTouchInput { down(center) }
        advanceHold(3_200)
        compose.waitUntil(2_000) { calls.get() == 1 }
        SystemClock.sleep(1_000)
        compose.mainClock.advanceTimeBy(1_000)
        assertEquals(1, calls.get())
        title().performTouchInput { up() }
    }

    @Test fun fingerDriftDuringThreeSecondHoldStillTriggers() {
        showHistory()
        title().performTouchInput {
            down(center)
            moveBy(Offset(18f, 10f))
            moveBy(Offset(-8f, 6f))
        }
        advanceHold(3_200)
        compose.waitUntil(2_000) { calls.get() == 1 }
        assertEquals(1, calls.get())
        title().performTouchInput { up() }
    }

    @Test fun holdBelowThresholdDoesNotTriggerButCrossingThreeSecondsDoes() {
        showHistory()
        title().performTouchInput { down(center) }
        advanceHold(2_800)
        assertEquals(0, calls.get())
        advanceHold(400)
        compose.waitUntil(2_000) { calls.get() == 1 }
        title().performTouchInput { up() }
    }

    @Test fun earlyReleaseAndDisabledHeaderDoNotTrigger() {
        showHistory()
        repeat(2) {
            title().performTouchInput { down(center) }
            SystemClock.sleep(100)
            compose.mainClock.advanceTimeBy(100)
            title().performTouchInput { up() }
        }

        title().performTouchInput { down(center) }
        compose.runOnIdle { enabled.value = false }
        compose.mainClock.advanceTimeByFrame()
        advanceHold(3_200)
        title().performTouchInput { up() }
        assertEquals(0, calls.get())
    }

    @Test fun clearHistoryRequiresExplicitConfirmation() {
        showHistory()
        compose.mainClock.autoAdvance = true

        compose.onNodeWithTag("calculator_history_clear").performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(listOf(CalculatorHistoryEntry("1+1", "2")), history.value)
        }

        compose.onNodeWithTag("calculator_history_clear_confirm").performClick()
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(emptyList<CalculatorHistoryEntry>(), history.value) }
    }
}
