package com.pulse.bluetoothdisable.cover

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val COVER_RECOVERY_HOLD_MILLIS = 7_000L

/**
 * Passive seven-second hold listener used by cover recovery targets.
 *
 * The gesture intentionally ignores touch slop, pointer consumption and small movement so the
 * recovery hold remains reliable across OEM devices and parent components such as drawers and
 * buttons. Releasing the original pointer before the timeout cancels the hold.
 */
@Composable
internal fun Modifier.coverRecoveryHold(enabled: Boolean, onHold: () -> Unit): Modifier {
    val currentOnHold = rememberUpdatedState(onHold)

    return pointerInput(enabled) {
        if (!enabled) return@pointerInput

        coroutineScope {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var released = false

                val timer = launch {
                    delay(COVER_RECOVERY_HOLD_MILLIS)
                    if (!released) currentOnHold.value()
                }

                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!pointer.pressed) break
                    }
                } finally {
                    released = true
                    timer.cancel()
                }
            }
        }
    }
}
