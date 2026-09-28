package com.pulse.bluetoothdisable.admin

import android.app.admin.DevicePolicyManager
import android.content.Context
import com.pulse.bluetoothdisable.domain.DeviceOwnerStatus

class DeviceOwnerManager(context: Context) : DeviceOwnerStatus {
    private val appContext = context.applicationContext
    private val devicePolicyManager = appContext.getSystemService(DevicePolicyManager::class.java)

    override fun isDeviceOwner(): Boolean =
        devicePolicyManager.isDeviceOwnerApp(appContext.packageName)
}
