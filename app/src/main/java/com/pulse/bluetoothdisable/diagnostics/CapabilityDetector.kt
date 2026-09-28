package com.pulse.bluetoothdisable.diagnostics

import android.content.Context
import android.content.pm.PackageManager
import com.pulse.bluetoothdisable.domain.BluetoothCapability

class CapabilityDetector(context: Context) : BluetoothCapability {
    private val packageManager = context.applicationContext.packageManager

    override fun isBluetoothSupported(): Boolean =
        packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
}
