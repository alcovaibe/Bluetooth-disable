package com.pulse.bluetoothdisable.cover

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.pulse.bluetoothdisable.MainActivity
import com.pulse.bluetoothdisable.cover.calculator.CalculatorCoverActivity
import com.pulse.bluetoothdisable.cover.calendar.CalendarCoverActivity
import com.pulse.bluetoothdisable.launcher.LauncherIconController
import com.pulse.bluetoothdisable.launcher.LauncherStyle
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoverModeNavigationInstrumentedTest {
    private lateinit var context: Context
    private lateinit var manager: CoverModeManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = CoverModeManager(context)
        manager.resetToDefault()
    }

    @After
    fun tearDown() {
        manager.resetToDefault()
    }

    @Test
    fun launcherAliasesRouteToExpectedActivities() {
        assertEquals(
            MainActivity::class.java.name,
            aliasTarget(".LauncherAlias"),
        )
        assertEquals(
            CalculatorCoverActivity::class.java.name,
            aliasTarget(".LauncherAliasCalculator"),
        )
        assertEquals(
            CalendarCoverActivity::class.java.name,
            aliasTarget(".LauncherAliasCalendar"),
        )
    }

    @Test
    fun calculatorActivationSwitchesLauncherAtomically() {
        manager.activateCalculator("58317")

        assertEquals(CoverMode.CALCULATOR, manager.activeMode())
        assertEquals(
            LauncherStyle.CALCULATOR,
            LauncherIconController(context).selectedStyle(),
        )
        assertTrue(manager.isCalculatorReady())
    }

    @Test
    fun ordinaryMainLaunchHasNoCoverOrigin() {
        val ordinaryIntent = Intent(context, MainActivity::class.java)
        assertNull(CoverModeNavigator.coverOrigin(context, ordinaryIntent))
    }

    @Test
    fun coverUnlockMarksOriginAndHideDoesNotRevealMainOnBack() {
        manager.activateCalculator("58317")
        val coverIntent = Intent(context, CalculatorCoverActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        ActivityScenario.launch<CalculatorCoverActivity>(coverIntent).use { scenario ->
            scenario.onActivity { activity ->
                CoverModeNavigator.openMainFromCover(activity, CoverMode.CALCULATOR)
            }
            waitForIdle()

            val mainActivity = resumedActivities().filterIsInstance<MainActivity>().single()
            assertEquals(
                CoverMode.CALCULATOR,
                CoverModeNavigator.coverOrigin(mainActivity, mainActivity.intent),
            )

            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                CoverModeNavigator.hideToCoverMode(mainActivity, CoverMode.CALCULATOR)
            }
            waitForIdle()

            assertTrue(resumedActivities().any { it is CalculatorCoverActivity })
            assertFalse(resumedActivities().any { it is MainActivity })

            Espresso.pressBackUnconditionally()
            waitForIdle()

            assertFalse(resumedActivities().any { it is MainActivity })
        }
    }

    @Suppress("DEPRECATION")
    private fun aliasTarget(aliasSuffix: String): String? {
        val info = context.packageManager.getActivityInfo(
            ComponentName(context, "${context.packageName}$aliasSuffix"),
            PackageManager.MATCH_DISABLED_COMPONENTS,
        )
        return info.targetActivity
    }

    private fun waitForIdle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun resumedActivities(): Collection<Activity> {
        val result = AtomicReference<Collection<Activity>>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result.set(
                ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .toList(),
            )
        }
        return result.get()
    }
}
