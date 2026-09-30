package com.pulse.bluetoothdisable.cover.calendar

import com.pulse.bluetoothdisable.cover.CoverMode
import com.pulse.bluetoothdisable.cover.CoverRecoveryManager
import org.junit.Assert.*
import org.junit.Test

class CalendarCoverRecoveryManagerTest {
    private var mode = CoverMode.CALENDAR
    private var resets = 0
    private fun manager() = CoverRecoveryManager({ mode }, { resets++; mode = CoverMode.DEFAULT }, modeToRecover = CoverMode.CALENDAR)

    @Test fun unsupportedRecoveryTargetsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CoverRecoveryManager({ CoverMode.DEFAULT }, {}, modeToRecover = CoverMode.DEFAULT)
        }
    }

    @Test fun notesAndGalleryRecoveryTargetsAreSupported() {
        for (target in listOf(CoverMode.NOTES, CoverMode.GALLERY)) {
            var current = target
            var targetResets = 0
            val recovery = CoverRecoveryManager(
                { current },
                { targetResets++; current = CoverMode.DEFAULT },
                modeToRecover = target,
            )
            val id = recovery.begin()
            assertNotNull(id)
            recovery.authenticationSucceeded(id!!)
            assertTrue(recovery.confirmReset())
            assertEquals(1, targetResets)
            assertEquals(CoverMode.DEFAULT, current)
        }
    }

    @Test fun cancelledOrFailedAuthenticationCannotReset() {
        for (cancelled in listOf(true, false)) {
            val recovery = manager()
            val id = recovery.begin()!!
            if (cancelled) recovery.cancel() else recovery.authenticationRejected(id)
            recovery.authenticationSucceeded(id) // A late success must be ignored.
            assertFalse(recovery.confirmReset())
            assertEquals(0, resets)
            assertEquals(CoverMode.CALENDAR, mode)
        }
    }

    @Test fun cancellingConfirmationDoesNotReset() {
        val recovery = manager()
        recovery.authenticationSucceeded(recovery.begin()!!)
        assertEquals(CoverRecoveryManager.State.CONFIRMING, recovery.state)
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
        for (other in CoverMode.entries.filterNot { it == CoverMode.CALENDAR }) {
            mode = other
            assertNull(recovery.begin())
        }
        mode = CoverMode.CALENDAR
        val id = recovery.begin()!!
        mode = CoverMode.CALCULATOR
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
        assertEquals(CoverRecoveryManager.State.AUTHENTICATING, recovery.state)
        recovery.authenticationSucceeded(current)
        assertTrue(recovery.confirmReset())
    }

    @Test fun modeChangedAfterAuthenticationCannotBeReset() {
        val recovery = manager()
        recovery.authenticationSucceeded(recovery.begin()!!)
        mode = CoverMode.CALCULATOR
        assertFalse(recovery.confirmReset())
        assertEquals(0, resets)
        assertEquals(CoverMode.CALCULATOR, mode)
    }

    @Test fun recreatedManagerRequiresFreshAuthentication() {
        val old = manager()
        old.authenticationSucceeded(old.begin()!!)
        val recreated = manager()
        assertFalse(recreated.confirmReset())
        assertEquals(0, resets)
    }

    @Test fun resetFailureReturnsToIdleForRetry() {
        val recovery = CoverRecoveryManager({ mode }, { error("failure") }, modeToRecover = CoverMode.CALENDAR)
        recovery.authenticationSucceeded(recovery.begin()!!)
        try {
            recovery.confirmReset()
            fail("Expected reset failure")
        } catch (_: IllegalStateException) { }
        assertEquals(CoverRecoveryManager.State.IDLE, recovery.state)
        assertEquals(CoverMode.CALENDAR, mode)
        assertNotNull(recovery.begin())
    }
}
