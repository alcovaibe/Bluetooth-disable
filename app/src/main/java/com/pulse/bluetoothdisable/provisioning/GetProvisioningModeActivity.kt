package com.pulse.bluetoothdisable.provisioning

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Build
import android.os.Bundle

/**
 * Android 12+ admin-integrated provisioning entry point.
 *
 * This DPC supports only fully managed devices. The Setup Wizard invokes this
 * activity and expects the selected provisioning mode in the result intent.
 */
class GetProvisioningModeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val allowedModes = intent.getIntegerArrayListExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_ALLOWED_PROVISIONING_MODES,
        )

        val fullyManagedAllowed = allowedModes.isNullOrEmpty() ||
            allowedModes.contains(DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE)

        if (!fullyManagedAllowed) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val result = Intent().putExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_MODE,
            DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE,
        )

        setResult(RESULT_OK, result)
        finish()
    }
}
