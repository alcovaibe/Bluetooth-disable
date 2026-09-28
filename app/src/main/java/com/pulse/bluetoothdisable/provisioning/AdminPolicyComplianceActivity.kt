package com.pulse.bluetoothdisable.provisioning

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle

/**
 * Provisioning finalization entry point used by Android Setup Wizard.
 *
 * For V1 there is no additional compliance UI: provisioning is accepted only
 * after Android has actually made this package the Device Owner.
 */
class AdminPolicyComplianceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val devicePolicyManager = getSystemService(DevicePolicyManager::class.java)
        val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(packageName)

        setResult(
            if (isDeviceOwner) RESULT_OK else RESULT_CANCELED,
            Intent(),
        )
        finish()
    }
}
