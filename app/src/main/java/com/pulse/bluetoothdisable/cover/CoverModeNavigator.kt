package com.pulse.bluetoothdisable.cover

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.pulse.bluetoothdisable.MainActivity
import com.pulse.bluetoothdisable.cover.calculator.CalculatorCoverActivity

object CoverModeNavigator {
    private const val EXTRA_COVER_ORIGIN =
        "com.pulse.bluetoothdisable.extra.INTERNAL_COVER_ORIGIN"

    fun openMainFromCover(activity: Activity, mode: CoverMode) {
        require(mode == CoverMode.CALCULATOR)
        if (CoverModeManager(activity).activeMode() != mode) return

        activity.startActivity(
            Intent(activity, MainActivity::class.java).apply {
                putExtra(EXTRA_COVER_ORIGIN, mode.name)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
        )
    }

    fun coverOrigin(context: Context, intent: Intent?): CoverMode? {
        val raw = intent?.getStringExtra(EXTRA_COVER_ORIGIN) ?: return null
        val mode = CoverMode.entries.firstOrNull { it.name == raw } ?: return null
        if (mode != CoverMode.CALCULATOR) return null
        return mode.takeIf { CoverModeManager(context).activeMode() == it }
    }

    fun hideToCoverMode(activity: Activity, mode: CoverMode) {
        val coverActivity = coverActivityClass(mode) ?: return
        activity.startActivity(
            Intent(activity, coverActivity).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            },
        )
    }

    private fun coverActivityClass(mode: CoverMode): Class<out Activity>? = when (mode) {
        CoverMode.CALCULATOR -> CalculatorCoverActivity::class.java
        CoverMode.DEFAULT,
        CoverMode.CALENDAR,
        CoverMode.NOTES,
        CoverMode.GALLERY -> null
    }
}
