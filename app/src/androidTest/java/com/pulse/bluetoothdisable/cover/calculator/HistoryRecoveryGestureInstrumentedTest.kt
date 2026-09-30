package com.pulse.bluetoothdisable.cover.calculator

import android.os.SystemClock
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
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
                CalculatorHistoryDrawer(history.value, false, { history.value = emptyList() },
                    enabled.value, { calls.incrementAndGet() })
            }
        }
    }
    private fun title() = compose.onNodeWithText(context.getString(R.string.calculator_history))

    @Test fun sevenSecondHoldFiresOnceEvenWhenFingerRemainsDown() {
        showHistory()
        title().performTouchInput { down(center) }
        SystemClock.sleep(7_200)
        compose.waitUntil(2_000) { calls.get() == 1 }
        SystemClock.sleep(3_000)
        assertEquals(1, calls.get())
        title().performTouchInput { up() }
    }

    @Test fun releaseMovementCancellationAndDisabledHeaderDoNotTrigger() {
        showHistory()
        repeat(2) {
            title().performTouchInput { down(center) }
            SystemClock.sleep(100)
            title().performTouchInput { up() }
        }
        title().performTouchInput { down(center); moveTo(center.copy(x = -100f)); up() }
        title().performTouchInput { down(center) }
        compose.runOnIdle { enabled.value = false }
        SystemClock.sleep(7_200)
        title().performTouchInput { up() }
        assertEquals(0, calls.get())
        compose.onNodeWithText(context.getString(R.string.calculator_clear_history)).performClick()
        compose.runOnIdle { assertEquals(emptyList<CalculatorHistoryEntry>(), history.value) }
    }
}
