package com.pulse.bluetoothdisable.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionEngineTest {
    @Test
    fun `reports not provisioned when app is not device owner`() {
        val engine = engine(deviceOwner = false)

        val result = engine.currentState()

        assertEquals(
            ProtectionResult.Success(ProtectionState.NOT_PROVISIONED),
            result,
        )
    }

    @Test
    fun `reports ready when device owner has no bluetooth restriction`() {
        val engine = engine(deviceOwner = true, policyEnabled = false)

        val result = engine.currentState()

        assertEquals(ProtectionResult.Success(ProtectionState.READY), result)
    }

    @Test
    fun `enable protection verifies that policy actually became active`() {
        val policy = FakeBluetoothPolicy(enabled = false)
        val engine = engine(deviceOwner = true, policy = policy)

        val result = engine.enableProtection()

        assertEquals(ProtectionResult.Success(ProtectionState.PROTECTED), result)
        assertTrue(policy.enabled)
    }

    @Test
    fun `enable protection fails when restriction does not stick`() {
        val policy = FakeBluetoothPolicy(
            enabled = false,
            enableChangesState = false,
        )
        val engine = engine(deviceOwner = true, policy = policy)

        val result = engine.enableProtection()

        assertTrue(result is ProtectionResult.Failure)
    }

    private fun engine(
        deviceOwner: Boolean,
        bluetoothSupported: Boolean = true,
        policyEnabled: Boolean = false,
        policy: FakeBluetoothPolicy = FakeBluetoothPolicy(policyEnabled),
    ): ProtectionEngine = ProtectionEngine(
        deviceOwnerStatus = object : DeviceOwnerStatus {
            override fun isDeviceOwner(): Boolean = deviceOwner
        },
        bluetoothCapability = object : BluetoothCapability {
            override fun isBluetoothSupported(): Boolean = bluetoothSupported
        },
        bluetoothPolicy = policy,
    )

    private class FakeBluetoothPolicy(
        var enabled: Boolean,
        private val enableChangesState: Boolean = true,
    ) : BluetoothPolicyController {
        override fun isProtectionEnabled(): Boolean = enabled

        override fun enableProtection() {
            if (enableChangesState) enabled = true
        }

        override fun disableProtection() {
            enabled = false
        }
    }
}
