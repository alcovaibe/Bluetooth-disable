package com.pulse.bluetoothdisable.policy

import android.Manifest
import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import com.pulse.bluetoothdisable.admin.AppDeviceAdminReceiver
import com.pulse.bluetoothdisable.domain.BluetoothPolicyController

class AndroidBluetoothPolicyController(context: Context) : BluetoothPolicyController {
    private val appContext = context.applicationContext
    private val devicePolicyManager = appContext.getSystemService(DevicePolicyManager::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val adminComponent = ComponentName(appContext, AppDeviceAdminReceiver::class.java)

    init {
        // Pre-grant only the one runtime Bluetooth permission used by the fast shutdown path.
        // A non-Device-Owner installation never receives this automatic grant.
        ensureFastShutdownPermission()
    }

    /**
     * Reads the effective restriction seen by the current user rather than only the
     * restriction bundle attributed to this admin. This remains correct when
     * DISALLOW_BLUETOOTH is applied globally by a Device Owner on modern Android.
     */
    override fun isProtectionEnabled(): Boolean =
        userManager.hasUserRestriction(UserManager.DISALLOW_BLUETOOTH)

    override fun enableProtection() {
        // The persistent policy is always applied first. The direct adapter shutdown below
        // only makes the visible transition faster and is deliberately best-effort.
        devicePolicyManager.addUserRestriction(
            adminComponent,
            UserManager.DISALLOW_BLUETOOTH,
        )

        requestImmediateAdapterShutdown()
    }

    override fun disableProtection() {
        devicePolicyManager.clearUserRestriction(
            adminComponent,
            UserManager.DISALLOW_BLUETOOTH,
        )
    }

    private fun ensureFastShutdownPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true

        if (appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }

        if (!devicePolicyManager.isDeviceOwnerApp(appContext.packageName)) return false

        val accepted = devicePolicyManager.setPermissionGrantState(
            adminComponent,
            appContext.packageName,
            Manifest.permission.BLUETOOTH_CONNECT,
            DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED,
        )

        return accepted &&
            appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun requestImmediateAdapterShutdown() {
        if (!ensureFastShutdownPermission()) return

        // Privacy boundary: do not scan, enumerate bonded devices, advertise, connect to a
        // remote device, or open Bluetooth profiles/sockets. The adapter is only asked to turn off.
        runCatching {
            bluetoothManager.adapter?.disable()
        }
    }
}
