package com.pulse.bluetoothdisable.cover.calculator

import com.pulse.bluetoothdisable.cover.CoverMode

/** No UI or authentication implementation here: only a gated, one-at-a-time flow.
 * Authentication permits are in memory and never survive Activity/process recreation.
 */
class CalculatorCoverRecoveryManager(
    private val activeMode: () -> CoverMode,
    private val resetCalculator: () -> Unit,
    private val onStateChanged: (State) -> Unit = {},
) {
    enum class State { IDLE, AUTHENTICATING, CONFIRMING, RESETTING }
    var state = State.IDLE
        private set
    private var attempt = 0L

    fun begin(): Long? {
        if (state != State.IDLE || activeMode() != CoverMode.CALCULATOR) return null
        attempt++
        changeState(State.AUTHENTICATING)
        return attempt
    }

    fun authenticationSucceeded(id: Long) {
        if (id != attempt || state != State.AUTHENTICATING) return
        if (activeMode() != CoverMode.CALCULATOR) cancel() else changeState(State.CONFIRMING)
    }

    fun authenticationRejected(id: Long) {
        if (id == attempt && state == State.AUTHENTICATING) cancel()
    }

    fun cancel() {
        attempt++ // Late callbacks cannot authorize a new attempt.
        changeState(State.IDLE)
    }

    fun confirmReset(): Boolean {
        if (state != State.CONFIRMING || activeMode() != CoverMode.CALCULATOR) {
            cancel()
            return false
        }
        changeState(State.RESETTING)
        try {
            resetCalculator()
            return true
        } finally {
            cancel()
        }
    }

    private fun changeState(next: State) {
        state = next
        onStateChanged(next)
    }
}
