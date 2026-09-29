package com.pulse.bluetoothdisable.cover

import android.content.Context
import com.pulse.bluetoothdisable.cover.calculator.CalculatorAccessCodeManager
import com.pulse.bluetoothdisable.launcher.LauncherIconController
import com.pulse.bluetoothdisable.launcher.LauncherStyle

class CoverModeManager(context: Context) {
    private val appContext = context.applicationContext
    private val store = CoverModeStore(appContext)
    private val launcher = LauncherIconController(appContext)
    private val accessCodeManager = CalculatorAccessCodeManager(appContext)

    fun activeMode(): CoverMode = store.activeMode()

    fun isCalculatorReady(): Boolean =
        store.activeMode() == CoverMode.CALCULATOR &&
            launcher.selectedStyle() == LauncherStyle.CALCULATOR &&
            accessCodeManager.hasCode()

    fun activateCalculator(code: String) {
        store.markPending(CoverMode.CALCULATOR)
        try {
            accessCodeManager.setCode(code)
            launcher.setStyle(LauncherStyle.CALCULATOR)
            store.setActiveMode(CoverMode.CALCULATOR)
            store.clearPending()
        } catch (error: Exception) {
            resetToDefault()
            throw error
        }
    }

    fun deactivateToLauncher(style: LauncherStyle) {
        accessCodeManager.clearCode()
        store.setActiveMode(CoverMode.DEFAULT)
        store.clearPending()
        launcher.setStyle(style)
    }

    fun resetToDefault() {
        accessCodeManager.clearCode()
        try {
            store.setActiveMode(CoverMode.DEFAULT)
            store.clearPending()
        } finally {
            launcher.setStyle(LauncherStyle.DEFAULT)
        }
    }

    fun recoverInterruptedSetup() {
        if (store.pendingMode() != null) {
            resetToDefault()
            return
        }

        when (store.activeMode()) {
            CoverMode.CALCULATOR -> {
                if (!isCalculatorReady()) resetToDefault()
            }
            CoverMode.DEFAULT -> {
                // Upgrade safety for 1.0.5: Calculator used to be only a launcher style.
                // 1.0.6 must not enter CalculatorCoverActivity without a configured code.
                if (launcher.selectedStyle() == LauncherStyle.CALCULATOR) {
                    resetToDefault()
                }
            }
            CoverMode.CALENDAR,
            CoverMode.NOTES,
            CoverMode.GALLERY -> resetToDefault()
        }
    }
}
