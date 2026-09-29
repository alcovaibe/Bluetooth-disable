package com.pulse.bluetoothdisable

import android.app.Application
import com.pulse.bluetoothdisable.transfer.DeviceTransferGuard

class BluetoothDisablerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Run before activities/services read local preferences. If an OEM migration
        // tool copied app-private data from another device, reset it to clean-install state.
        DeviceTransferGuard.enforce(this)
    }
}
