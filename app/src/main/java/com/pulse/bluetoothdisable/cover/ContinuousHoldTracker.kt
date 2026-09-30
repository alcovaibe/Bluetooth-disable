package com.pulse.bluetoothdisable.cover

/** Monotonic timestamps; each pointer sequence has its own one-shot deadline. */
class ContinuousHoldTracker {
    private var startedAt: Long? = null
    private var triggered = false

    fun start(nowMillis: Long) {
        startedAt = nowMillis
        triggered = false
    }

    fun cancel() {
        startedAt = null
        triggered = false
    }

    fun advance(nowMillis: Long): Boolean {
        val start = startedAt ?: return false
        if (triggered || nowMillis - start < HOLD_MILLIS) return false
        triggered = true
        return true
    }

    companion object {
        const val HOLD_MILLIS = 7_000L
    }
}
