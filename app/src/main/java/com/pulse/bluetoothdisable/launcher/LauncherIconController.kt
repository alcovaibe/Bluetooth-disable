package com.pulse.bluetoothdisable.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

class LauncherIconController(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val launcherAlias = ComponentName(
        appContext,
        "com.pulse.bluetoothdisable.LauncherAlias",
    )

    fun isHidden(): Boolean =
        packageManager.getComponentEnabledSetting(launcherAlias) ==
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED

    fun hide() {
        packageManager.setComponentEnabledSetting(
            launcherAlias,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

    fun show() {
        packageManager.setComponentEnabledSetting(
            launcherAlias,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
    }
}
