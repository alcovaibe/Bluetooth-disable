package com.pulse.bluetoothdisable.cover.calculator

import com.pulse.bluetoothdisable.cover.CoverMode
import org.junit.Assert.*
import org.junit.Test

class CalculatorCoverRecoveryManagerTest {
    private var mode = CoverMode.CALCULATOR
    private var resets = 0
    private fun manager() = CalculatorCoverRecoveryManager({ mode }, { resets++; mode = CoverMode.DEFAULT })

    @Test fun cancelledOrFailedAuthenticationCannotReset() {
        for (cancelled in listOf(true, false)) {
            val recovery = manager()
            val id = recovery.begin()!!
            if (cancelled) recovery.cancel() else recovery.authenticationRejected(id)
            recovery.authenticationSucceeded(id) // A late success must be ignored.
            assertFalse(recovery.confirmReset())
            assertEquals(0, resets)
            assertEquals(CoverMode.CALCULATOR, mode)
        }
    }

    @Test fun cancellingConfirmationDoesNotReset() {
        val recovery = manager()
        recovery.authenticationSucceeded(recovery.begin()!!)
        assertEquals(CalculatorCoverRecoveryManager.State.CONFIRMING, recovery.state)
        recovery.cancel()
        assertFalse(recovery.confirmReset())
        assertEquals(0, resets)
    }

    @Test fun successfulAuthenticationAndConfirmationResetOnce() {
        val recovery = manager()
        recovery.authenticationSucceeded(recovery.begin()!!)
        assertTrue(recovery.confirmReset())
        assertFalse(recovery.confirmReset())
        assertEquals(1, resets)
        assertEquals(CoverMode.DEFAULT, mode)
    }

    @Test fun gestureAloneCannotResetAndDuplicatePromptsAreRejected() {
        val recovery = manager()
        recovery.begin()
        assertNull(recovery.begin())
        assertFalse(recovery.confirmReset())
        assertEquals(0, resets)
    }

    @Test fun otherModesAndModeChangedDuringAuthenticationAreRejected() {
        val recovery = manager()
        for (other in CoverMode.entries.filterNot { it == CoverMode.CALCULATOR }) {
            mode = other
            assertNull(recovery.begin())
        }
        mode = CoverMode.CALCULATOR
        val id = recovery.begin()!!
        mode = CoverMode.CALENDAR
        recovery.authenticationSucceeded(id)
        assertFalse(recovery.confirmReset())
        assertEquals(0, resets)
    }

    @Test fun staleAuthenticationCannotAuthorizeNextAttempt() {
        val recovery = manager()
        val old = recovery.begin()!!
        recovery.cancel()
        val current = recovery.begin()!!
        recovery.authenticationSucceeded(old)
        assertEquals(CalculatorCoverRecoveryManager.State.AUTHENTICATING, recovery.state)
        recovery.authenticationSucceeded(current)
        assertTrue(recovery.confirmReset())
    }

    @Test fun modeChangedAfterAuthenticationCannotBeReset() {
        val recovery = manager()
        recovery.authenticationSucceeded(recovery.begin()!!)
        mode = CoverMode.CALENDAR
        assertFalse(recovery.confirmReset())
        assertEquals(0, resets)
        assertEquals(CoverMode.CALENDAR, mode)
    }

    @Test fun recreatedManagerRequiresFreshAuthentication() {
        val old = manager()
        old.authenticationSucceeded(old.begin()!!)
        val recreated = manager()
        assertFalse(recreated.confirmReset())
        assertEquals(0, resets)
    }

    @Test fun resetFailureReturnsToIdleForRetry() {
        val recovery = CalculatorCoverRecoveryManager({ mode }, { error("failure") })
        recovery.authenticationSucceeded(recovery.begin()!!)
        try {
            recovery.confirmReset()
            fail("Expected reset failure")
        } catch (_: IllegalStateException) { }
        assertEquals(CalculatorCoverRecoveryManager.State.IDLE, recovery.state)
        assertEquals(CoverMode.CALCULATOR, mode)
        assertNotNull(recovery.begin())
    }
}
