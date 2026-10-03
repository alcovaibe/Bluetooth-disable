package com.pulse.bluetoothdisable.cover.calendar

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pulse.bluetoothdisable.cover.CoverModeManager
import com.pulse.bluetoothdisable.localization.LanguageManager
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarAuditInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = CoverModeManager(context)
    private val date = LocalDate.of(2026, 10, 3)

    @Before fun before() {
        manager.resetToDefault()
        LanguageManager.setSelectedLanguage(context, LanguageManager.ENGLISH)
    }

    @After fun after() {
        manager.resetToDefault()
    }

    @Test fun monthNavigationAlwaysSelectsFirstDayOfDestinationMonth() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = CalendarViewModel(application)
        viewModel.select(LocalDate.of(2024, 1, 31))
        viewModel.moveMonth(1)
        assertEquals(LocalDate.of(2024, 2, 1), viewModel.uiState.selectedDate)
        viewModel.moveMonth(-1)
        assertEquals(LocalDate.of(2024, 1, 1), viewModel.uiState.selectedDate)
    }

    @Test fun newNoteDraftSurvivesActivityRestartAndRemainsBoundToItsDate() {
        manager.activateCalendar(date, "secret text")

        ActivityScenario.launch<CalendarCoverActivity>(
            Intent(context, CalendarCoverActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ).use { scenario ->
            scenario.onActivity { activity ->
                ViewModelProvider(activity)[CalendarViewModel::class.java].select(date)
            }
            compose.onNodeWithContentDescription("Add note").performClick()
            compose.onNodeWithTag("calendar_note_text").performTextInput("draft text")
            compose.onNodeWithText("CANCEL").performClick()
            compose.waitUntil(5_000) {
                CalendarDraftStore(context).text(date) == "draft text"
            }
        }

        ActivityScenario.launch<CalendarCoverActivity>(
            Intent(context, CalendarCoverActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ).use { scenario ->
            val otherDate = date.plusDays(1)
            scenario.onActivity { activity ->
                ViewModelProvider(activity)[CalendarViewModel::class.java].select(otherDate)
            }
            compose.onNodeWithContentDescription("Add note").performClick()
            assertEquals(
                "",
                compose.onNodeWithTag("calendar_note_text")
                    .fetchSemanticsNode().config[SemanticsProperties.EditableText].text,
            )
            compose.onNodeWithText("CANCEL").performClick()

            scenario.onActivity { activity ->
                ViewModelProvider(activity)[CalendarViewModel::class.java].select(date)
            }
            compose.onNodeWithContentDescription("Add note").performClick()
            assertEquals(
                "draft text",
                compose.onNodeWithTag("calendar_note_text")
                    .fetchSemanticsNode().config[SemanticsProperties.EditableText].text,
            )
        }
    }

    @Test fun twentySixthNoteIsBlockedBeforeEditorOpens() {
        manager.activateCalendar(date, "secret text")
        val repository = LocalCalendarNotesRepository(context)
        repeat(CalendarNotePolicy.MAX_NOTES_PER_DATE) { index ->
            repository.save(date, "note-$index")
        }

        ActivityScenario.launch<CalendarCoverActivity>(
            Intent(context, CalendarCoverActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ).use { scenario ->
            scenario.onActivity { activity ->
                ViewModelProvider(activity)[CalendarViewModel::class.java].select(date)
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("note-24").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Add note").performClick()
            compose.onNodeWithTag("calendar_note_text").assertDoesNotExist()
            compose.onNodeWithText("You can store up to 25 notes on one date.").assertIsDisplayed()
        }
    }
}
