package com.pulse.bluetoothdisable.cover.calendar

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pulse.bluetoothdisable.cover.CoverMode
import com.pulse.bluetoothdisable.cover.CoverModeManager
import com.pulse.bluetoothdisable.cover.CoverModeStore
import com.pulse.bluetoothdisable.cover.CoverRecoveryManager
import com.pulse.bluetoothdisable.cover.calculator.CalculatorAccessCodeManager
import com.pulse.bluetoothdisable.cover.calculator.CalculatorHistoryEntry
import com.pulse.bluetoothdisable.cover.calculator.CalculatorHistoryStore
import com.pulse.bluetoothdisable.launcher.LauncherIconController
import com.pulse.bluetoothdisable.launcher.LauncherStyle
import java.time.LocalDate
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarRecoveryInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val modes = CoverModeManager(context)
    private val access = CalendarAccessManager(context)
    private val date = LocalDate.of(2012, 12, 12)
    private val secret = "Private note"
    private val launcher = LauncherIconController(context)
    private val history = CalculatorHistoryStore(context)

    @Before fun before() {
        modes.resetToDefault()
        history.clear()
        modes.activateCalendar(date, secret)
        history.add("1+1", "2")
    }
    @After fun after() { modes.resetToDefault(); history.clear() }

    @Test fun authenticatedConfirmedRecoveryRestoresSingleDefaultAliasAndKeepsNotesAndHistory() {
        val notes = LocalCalendarNotesRepository(context)
        val note = notes.save(LocalDate.of(2024, 2, 29), "Regular note")
        val recovery = CoverRecoveryManager(modes::activeMode, modes::resetCalendarCover, modeToRecover = CoverMode.CALENDAR)
        recovery.authenticationSucceeded(recovery.begin()!!)
        assertTrue(recovery.confirmReset())
        assertEquals(CoverMode.DEFAULT, modes.activeMode())
        assertTrue(launcher.isExclusivelyEnabled(LauncherStyle.DEFAULT))
        assertFalse(access.hasRule())
        assertFalse(access.verify(date, secret))
        assertTrue(context.getSharedPreferences(CalendarAccessManager.PREFERENCES_NAME, 0).all.isEmpty())
        assertEquals(listOf(CalculatorHistoryEntry("1+1", "2")), history.entries())
        assertEquals(note, notes.notes().single())
    }

    @Test fun cancelledAuthenticationFailedAuthenticationAndCancelledConfirmationKeepAllState() {
        repeat(3) { step ->
            val recovery = CoverRecoveryManager(modes::activeMode, modes::resetCalendarCover, modeToRecover = CoverMode.CALENDAR)
            val id = recovery.begin()!!
            when (step) {
                0 -> recovery.cancel()
                1 -> recovery.authenticationRejected(id)
                2 -> { recovery.authenticationSucceeded(id); recovery.cancel() }
            }
            assertFalse(recovery.confirmReset())
            assertCalendarIntact()
        }
    }

    @Test fun failedModeCommitRollsBackVerifierAndAliases() {
        // Fail the mode write after the real aliases have switched and verifier was removed.
        val failing = FailingPreferencesContext(context, CoverModeStore.PREFERENCES_NAME, 2)
        assertThrows(IllegalStateException::class.java) {
            CoverModeManager(failing).resetCalendarCover()
        }
        assertCalendarIntact()
        assertNull(CoverModeStore(context).pendingMode())
    }

    @Test fun failedVerifierRemovalRestoresCalendar() {
        assertResetRollsBack(CalendarAccessManager.PREFERENCES_NAME, 1)
    }

    @Test fun failedLauncherPreferenceWriteRestoresCalendar() {
        assertResetRollsBack(LauncherIconController.PREFERENCES_NAME, 1)
    }

    @Test fun failedJournalCommitRestoresCalendarAfterAliasesAndModeChanged() {
        assertResetRollsBack(CoverModeStore.PREFERENCES_NAME, 3)
    }

    private fun assertResetRollsBack(preferencesName: String, failCommit: Int) {
        assertThrows(IllegalStateException::class.java) {
            CoverModeManager(FailingPreferencesContext(context, preferencesName, failCommit))
                .resetCalendarCover()
        }
        assertCalendarIntact()
        assertNull(CoverModeStore(context).pendingMode())
    }

    @Test fun interruptedResetJournalRestoresPreviousCalendarConfiguration() {
        val snapshot = JSONObject().apply {
            put("mode", CoverMode.CALENDAR.name)
            put("style", LauncherStyle.CALENDAR.name)
            put("hidden", false)
            put("calculator", JSONObject(context.getSharedPreferences(CalculatorAccessCodeManager.PREFERENCES_NAME, 0).all))
            put("calendar", JSONObject(context.getSharedPreferences(CalendarAccessManager.PREFERENCES_NAME, 0).all))
        }
        val store = CoverModeStore(context)
        store.beginTransition(CoverMode.DEFAULT, snapshot)
        access.clearVerifier()
        launcher.setStyle(LauncherStyle.DEFAULT)
        store.setActiveMode(CoverMode.DEFAULT)
        // Simulate process death before the journal's commit point.
        CoverModeManager(context).recoverInterruptedSetup()
        assertCalendarIntact()
        assertNull(store.pendingMode())
    }

    @Test fun resetRejectsCalculatorAndPreservesItsCode() {
        modes.activateCalculator("58317")
        assertThrows(IllegalStateException::class.java) { modes.resetCalendarCover() }
        assertEquals(CoverMode.CALCULATOR, modes.activeMode())
        assertTrue(launcher.isExclusivelyEnabled(LauncherStyle.CALCULATOR))
        assertTrue(CalculatorAccessCodeManager(context).verify("58317"))
    }

    private fun assertCalendarIntact() {
        assertEquals(CoverMode.CALENDAR, modes.activeMode())
        assertTrue(launcher.isExclusivelyEnabled(LauncherStyle.CALENDAR))
        assertTrue(access.verify(date, secret))
        assertEquals(listOf(CalculatorHistoryEntry("1+1", "2")), history.entries())
    }

    private class FailingPreferencesContext(
        base: Context,
        private val preferencesName: String,
        private val failCommit: Int,
    ) : ContextWrapper(base) {
        private var commits = 0
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            val original = super.getSharedPreferences(name, mode)
            if (name != preferencesName) return original
            return object : SharedPreferences by original {
                override fun edit(): SharedPreferences.Editor {
                    val delegate = original.edit()
                    return object : SharedPreferences.Editor by delegate {
                        override fun putString(key: String?, value: String?) = apply { delegate.putString(key, value) }
                        override fun remove(key: String?) = apply { delegate.remove(key) }
                        override fun clear() = apply { delegate.clear() }
                        override fun commit(): Boolean {
                            commits++
                            return if (commits == failCommit) false else delegate.commit()
                        }
                    }
                }
            }
        }
    }
}
