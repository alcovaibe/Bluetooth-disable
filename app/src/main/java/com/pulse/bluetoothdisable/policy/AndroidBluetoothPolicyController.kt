package com.pulse.bluetoothdisable.policy

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import com.pulse.bluetoothdisable.admin.AppDeviceAdminReceiver
import com.pulse.bluetoothdisable.domain.BluetoothPolicyController

class AndroidBluetoothPolicyController(context: Context) : BluetoothPolicyController {
    private val appContext = context.applicationContext
    private val devicePolicyManager = appContext.getSystemService(DevicePolicyManager::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val adminComponent = ComponentName(appContext, AppDeviceAdminReceiver::class.java)

    /**
     * Reads the effective restriction seen by the current user rather than only the
     * restriction bundle attributed to this admin. This remains correct when
     * DISALLOW_BLUETOOTH is applied globally by a Device Owner on modern Android.
     */
    override fun isProtectionEnabled(): Boolean =
        userManager.hasUserRestriction(UserManager.DISALLOW_BLUETOOTH)

    override fun enableProtection() {
        devicePolicyManager.addUserRestriction(
            adminComponent,
            UserManager.DISALLOW_BLUETOOTH,
        )
    }

    override fun disableProtection() {
        devicePolicyManager.clearUserRestriction(
            adminComponent,
            UserManager.DISALLOW_BLUETOOTH,
        )
    }
}
