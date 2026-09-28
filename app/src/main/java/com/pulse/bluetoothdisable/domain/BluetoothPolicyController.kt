package com.pulse.bluetoothdisable.domain

interface BluetoothPolicyController {
    fun isProtectionEnabled(): Boolean
    fun enableProtection()
    fun disableProtection()
}
