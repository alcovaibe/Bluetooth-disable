package com.pulse.bluetoothdisable.admin

import android.app.admin.DeviceAdminReceiver

/**
 * Minimal DeviceAdminReceiver required for Device Owner provisioning.
 *
 * Bluetooth policy changes are deliberately kept out of receiver callbacks.
 * The receiver only establishes the DPC component recognized by Android.
 */
class AppDeviceAdminReceiver : DeviceAdminReceiver()
