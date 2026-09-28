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
    private val adminComponent = ComponentName(appContext, AppDeviceAdminReceiver::class.java)

    override fun isProtectionEnabled(): Boolean =
        devicePolicyManager
            .getUserRestrictions(adminComponent)
            .getBoolean(UserManager.DISALLOW_BLUETOOTH, false)

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
