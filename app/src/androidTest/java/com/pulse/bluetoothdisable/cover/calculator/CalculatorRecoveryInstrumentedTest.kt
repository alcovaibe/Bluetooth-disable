package com.pulse.bluetoothdisable.cover.calculator

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pulse.bluetoothdisable.cover.CoverMode
import com.pulse.bluetoothdisable.cover.CoverModeManager
import com.pulse.bluetoothdisable.cover.CoverModeStore
import com.pulse.bluetoothdisable.cover.calendar.CalendarAccessManager
import com.pulse.bluetoothdisable.cover.calendar.LocalCalendarNotesRepository
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
class CalculatorRecoveryInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val modes = CoverModeManager(context)
    private val access = CalculatorAccessCodeManager(context)
    private val launcher = LauncherIconController(context)
    private val history = CalculatorHistoryStore(context)

    @Before fun before() {
        modes.resetToDefault()
        history.clear()
        modes.activateCalculator("58317")
        history.add("1+1", "2")
    }
    @After fun after() { modes.resetToDefault(); history.clear() }

    @Test fun authenticatedConfirmedRecoveryRestoresSingleDefaultAliasAndKeepsNotesAndHistory() {
        val notes = LocalCalendarNotesRepository(context)
        val note = notes.save(LocalDate.of(2024, 2, 29), "Regular note")
        val recovery = CalculatorCoverRecoveryManager(modes::activeMode, modes::resetCalculatorCover)
        recovery.authenticationSucceeded(recovery.begin()!!)
        assertTrue(recovery.confirmReset())
        assertEquals(CoverMode.DEFAULT, modes.activeMode())
        assertTrue(launcher.isExclusivelyEnabled(LauncherStyle.DEFAULT))
        assertFalse(access.hasCode())
        assertFalse(access.verify("58317"))
        assertTrue(context.getSharedPreferences(CalculatorAccessCodeManager.PREFERENCES_NAME, 0).all.isEmpty())
        assertEquals(listOf(CalculatorHistoryEntry("1+1", "2")), history.entries())
        assertEquals(note, notes.notes().single())
    }

    @Test fun cancelledAuthenticationFailedAuthenticationAndCancelledConfirmationKeepAllState() {
        repeat(3) { step ->
            val recovery = CalculatorCoverRecoveryManager(modes::activeMode, modes::resetCalculatorCover)
            val id = recovery.begin()!!
            when (step) {
                0 -> recovery.cancel()
                1 -> recovery.authenticationRejected(id)
                2 -> { recovery.authenticationSucceeded(id); recovery.cancel() }
            }
            assertFalse(recovery.confirmReset())
            assertCalculatorIntact()
        }
    }

    @Test fun failedModeCommitRollsBackVerifierAndAliases() {
        // Fail the mode write after the real aliases have switched and verifier was removed.
        val failing = FailingPreferencesContext(context, CoverModeStore.PREFERENCES_NAME, 2)
        assertThrows(IllegalStateException::class.java) {
            CoverModeManager(failing).resetCalculatorCover()
        }
        assertCalculatorIntact()
        assertNull(CoverModeStore(context).pendingMode())
    }

    @Test fun failedVerifierRemovalRestoresCalculator() {
        assertResetRollsBack(CalculatorAccessCodeManager.PREFERENCES_NAME, 1)
    }

    @Test fun failedLauncherPreferenceWriteRestoresCalculator() {
        assertResetRollsBack(LauncherIconController.PREFERENCES_NAME, 1)
    }

    @Test fun failedJournalCommitRestoresCalculatorAfterAliasesAndModeChanged() {
        assertResetRollsBack(CoverModeStore.PREFERENCES_NAME, 3)
    }

    private fun assertResetRollsBack(preferencesName: String, failCommit: Int) {
        assertThrows(IllegalStateException::class.java) {
            CoverModeManager(FailingPreferencesContext(context, preferencesName, failCommit))
                .resetCalculatorCover()
        }
        assertCalculatorIntact()
        assertNull(CoverModeStore(context).pendingMode())
    }

    @Test fun interruptedResetJournalRestoresPreviousCalculatorConfiguration() {
        val snapshot = JSONObject().apply {
            put("mode", CoverMode.CALCULATOR.name)
            put("style", LauncherStyle.CALCULATOR.name)
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
        assertCalculatorIntact()
        assertNull(store.pendingMode())
    }

    @Test fun resetRejectsCalendarAndPreservesItsRule() {
        val date = LocalDate.of(2012, 12, 12)
        modes.activateCalendar(date, "Private note")
        assertThrows(IllegalStateException::class.java) { modes.resetCalculatorCover() }
        assertEquals(CoverMode.CALENDAR, modes.activeMode())
        assertTrue(launcher.isExclusivelyEnabled(LauncherStyle.CALENDAR))
        assertTrue(CalendarAccessManager(context).verify(date, "Private note"))
    }

    private fun assertCalculatorIntact() {
        assertEquals(CoverMode.CALCULATOR, modes.activeMode())
        assertTrue(launcher.isExclusivelyEnabled(LauncherStyle.CALCULATOR))
        assertTrue(access.verify("58317"))
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
