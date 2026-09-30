package com.pulse.bluetoothdisable.cover.calendar

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import java.io.File
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.pulse.bluetoothdisable.MainActivity
import com.pulse.bluetoothdisable.cover.CoverMode
import com.pulse.bluetoothdisable.cover.CoverModeManager
import com.pulse.bluetoothdisable.cover.CoverModeNavigator
import com.pulse.bluetoothdisable.cover.CoverModeStore
import com.pulse.bluetoothdisable.cover.calculator.CalculatorAccessCodeManager
import com.pulse.bluetoothdisable.launcher.LauncherIconController
import com.pulse.bluetoothdisable.launcher.LauncherStyle
import com.pulse.bluetoothdisable.localization.LanguageManager
import java.time.LocalDate
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarCoverInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = CoverModeManager(context)
    private val date = LocalDate.of(2012, 12, 12)

    @Before fun before() {
        manager.resetToDefault()
        LanguageManager.setSelectedLanguage(context, LanguageManager.ENGLISH)
    }
    @After fun after() { manager.resetToDefault() }

    @Test fun saveDoesNotUnlockButNoteTapOpensMainAndHideClearsTask() {
        manager.activateCalendar(date, "открой меня")
        ActivityScenario.launch<CalendarCoverActivity>(Intent(context, CalendarCoverActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)).use { scenario ->
            scenario.onActivity { activity ->
                ViewModelProvider(activity)[CalendarViewModel::class.java].select(date)
            }
            compose.waitUntil(5_000) { compose.onAllNodesWithText("No notes for this date.").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Add note").performClick()
            compose.onNodeWithTag("calendar_note_text").performTextInput("открой меня")
            compose.onNodeWithText("Save").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("calendar_note_text").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithText("открой меня").assertIsDisplayed()
            assertFalse(isMainResumed())
            capturePreview("calendar-saved-note.png")
            compose.onNodeWithText("открой меня").performClick()
            compose.waitUntil(5_000) { isMainResumed() }
            compose.onNodeWithText("HIDE").assertIsDisplayed().performClick()
            compose.waitUntil(5_000) { !isMainResumed() }
            compose.onNodeWithTag("calendar_day_${LocalDate.now()}").assertExists()
            capturePreview("calendar-after-hide.png")
            Espresso.pressBackUnconditionally()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertFalse(isMainResumed())
        }
    }

    @Test fun mismatchedDateAndCaseOpenOrdinaryEditor() {
        manager.activateCalendar(date, "открой меня")
        val repo = LocalCalendarNotesRepository(context)
        val wrongDate = repo.save(date.plusDays(1), "открой меня")
        val wrongText = repo.save(date, "Открой меня")
        ActivityScenario.launch<CalendarCoverActivity>(Intent(context, CalendarCoverActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)).use { scenario ->
            for (note in listOf(wrongDate, wrongText)) {
                scenario.onActivity { activity ->
                    ViewModelProvider(activity)[CalendarViewModel::class.java].select(note.date)
                }
                compose.waitUntil(5_000) { compose.onAllNodesWithText(note.text).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText(note.text).performClick()
                compose.waitUntil(5_000) { compose.onAllNodesWithTag("calendar_note_text").fetchSemanticsNodes().isNotEmpty() }
                assertFalse(isMainResumed())
                compose.onNodeWithText("CANCEL").performClick()
            }
        }
    }

    @Test fun setupActivatesOnlyAfterFinalConfirmationAndCreatesNoAccessNote() {
        ActivityScenario.launch<CalendarCoverSetupActivity>(Intent(context, CalendarCoverSetupActivity::class.java)).use {
            compose.onNode(hasSetTextAction()).performTextInput("my calendar text")
            compose.onNodeWithText("CONTINUE").performClick()
            assertEquals(CoverMode.DEFAULT, manager.activeMode())
            assertEquals(LauncherStyle.DEFAULT, LauncherIconController(context).selectedStyle())
            assertFalse(CalendarAccessManager(context).hasRule())
            compose.onNodeWithText("FINISH SETUP").performClick()
            compose.waitUntil(5_000) { manager.isCalendarReady() }
            assertTrue(CalendarAccessManager(context).verify(LocalDate.now(), "my calendar text"))
            assertTrue(LocalCalendarNotesRepository(context).notes().isEmpty())
        }
    }

    @Test fun cancellationDoesNotChangeExistingModeOrLauncher() {
        manager.activateCalculator("58317")
        ActivityScenario.launch<CalendarCoverSetupActivity>(Intent(context, CalendarCoverSetupActivity::class.java)).use {
            compose.onNodeWithText("CANCEL").performClick()
        }
        assertEquals(CoverMode.CALCULATOR, manager.activeMode())
        assertEquals(LauncherStyle.CALCULATOR, LauncherIconController(context).selectedStyle())
        assertTrue(CalculatorAccessCodeManager(context).verify("58317"))
        assertFalse(CalendarAccessManager(context).hasRule())
    }

    @Test fun modeSwitchClearsOnlyPreviousAccessAndResetClearsNotes() {
        manager.activateCalculator("58317")
        manager.activateCalendar(date, "открой меня")
        assertEquals(CoverMode.CALENDAR, manager.activeMode())
        assertTrue(manager.isCalendarReady())
        assertFalse(CalculatorAccessCodeManager(context).hasCode())
        val repo = LocalCalendarNotesRepository(context)
        repo.save(date, "Заметка")
        manager.activateCalculator("91347")
        assertTrue(manager.isCalculatorReady())
        assertFalse(CalendarAccessManager(context).hasRule())
        assertNull(CalendarAccessManager(context).secretDate())
        assertEquals(1, repo.notes().size)
        manager.resetToDefault()
        assertEquals(CoverMode.DEFAULT, manager.activeMode())
        assertTrue(repo.notes().isEmpty())
        assertEquals(LauncherStyle.DEFAULT, LauncherIconController(context).selectedStyle())
    }

    @Test fun interruptedCalendarActivationRestoresPreviousCalculator() {
        manager.activateCalculator("58317")
        val snapshot = snapshot()
        CoverModeStore(context).beginTransition(CoverMode.CALENDAR, snapshot)
        CalendarAccessManager(context).setAccessRule(date, "открой меня")
        LauncherIconController(context).setStyle(LauncherStyle.CALENDAR)
        CoverModeStore(context).setActiveMode(CoverMode.CALENDAR)
        manager.recoverInterruptedSetup()
        assertTrue(manager.isCalculatorReady())
        assertTrue(CalculatorAccessCodeManager(context).verify("58317"))
        assertFalse(CalendarAccessManager(context).hasRule())
    }

    @Test fun interruptedSameModeSetupRestoresPreviousVerifier() {
        manager.activateCalendar(date, "открой меня")
        CoverModeStore(context).beginTransition(CoverMode.CALENDAR, snapshot())
        CalendarAccessManager(context).setAccessRule(date.plusDays(1), "другой текст")
        manager.recoverInterruptedSetup()
        assertTrue(manager.isCalendarReady())
        assertTrue(CalendarAccessManager(context).verify(date, "открой меня"))
        assertFalse(CalendarAccessManager(context).verify(date.plusDays(1), "другой текст"))
    }

    private fun snapshot(): JSONObject = JSONObject().apply {
        put("mode", manager.activeMode().name)
        put("style", LauncherIconController(context).selectedStyle().name)
        put("hidden", false)
        for ((name, key) in listOf(
            CalculatorAccessCodeManager.PREFERENCES_NAME to "calculator",
            CalendarAccessManager.PREFERENCES_NAME to "calendar",
        )) {
            put(key, JSONObject().apply {
                context.getSharedPreferences(name, Context.MODE_PRIVATE).all.forEach { (key, value) -> put(key, value) }
            })
        }
    }

    private fun capturePreview(name: String) {
        compose.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val directory = File(context.getExternalFilesDir(null), "calendar-previews").apply { mkdirs() }
        File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun isMainResumed(): Boolean {
        var main: MainActivity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            main = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<MainActivity>().firstOrNull()
        }
        return main != null
    }
}
