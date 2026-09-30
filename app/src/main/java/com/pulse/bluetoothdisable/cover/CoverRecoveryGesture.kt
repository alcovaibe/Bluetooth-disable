package com.pulse.bluetoothdisable.cover

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Attached only to the chosen recovery target bounds. No click, long-click or recovery semantics. */
@Composable
internal fun Modifier.coverRecoveryHold(enabled: Boolean, onHold: () -> Unit): Modifier {
    val currentOnHold = rememberUpdatedState(onHold)
    return pointerInput(enabled) {
        if (!enabled) return@pointerInput
        coroutineScope {
            awaitEachGesture {
                val down = awaitFirstDown()
                val tracker = ContinuousHoldTracker().apply { start(SystemClock.uptimeMillis()) }
                val timer = launch {
                    delay(ContinuousHoldTracker.HOLD_MILLIS)
                    if (tracker.advance(SystemClock.uptimeMillis())) currentOnHold.value()
                }
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        val position = pointer.position
                        if (!pointer.pressed || pointer.isConsumed ||
                            event.changes.any { it.id != down.id && it.pressed } ||
                            position.x < 0 || position.y < 0 ||
                            position.x > size.width || position.y > size.height ||
                            (position - down.position).getDistance() > viewConfiguration.touchSlop
                        ) break
                        // Leave ordinary taps and drawer swipes available to the parent.
                        // A parent consuming movement cancels this hidden gesture.
                        val finalEvent = awaitPointerEvent(PointerEventPass.Final)
                        if (finalEvent.changes.any { it.isConsumed }) break
                    }
                } finally {
                    tracker.cancel()
                    timer.cancel()
                }
            }
        }
    }
}
