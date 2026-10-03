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
    fun `reports unsupported when bluetooth is unavailable`() {
        val engine = engine(deviceOwner = true, bluetoothSupported = false)

        val result = engine.currentState()

        assertEquals(ProtectionResult.Success(ProtectionState.UNSUPPORTED), result)
    }

    @Test
    fun `reports ready when device owner has no bluetooth restriction`() {
        val engine = engine(deviceOwner = true, policyEnabled = false)

        val result = engine.currentState()

        assertEquals(ProtectionResult.Success(ProtectionState.READY), result)
    }

    @Test
    fun `reports read failure when policy state cannot be read`() {
        val policy = FakeBluetoothPolicy(enabled = false, throwOnRead = true)
        val engine = engine(deviceOwner = true, policy = policy)

        val result = engine.currentState()

        assertFailure(ProtectionError.READ_POLICY, result)
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

        assertFailure(ProtectionError.ENABLE_NOT_CONFIRMED, result)
    }

    @Test
    fun `enable protection reports platform failure`() {
        val policy = FakeBluetoothPolicy(enabled = false, throwOnEnable = true)
        val engine = engine(deviceOwner = true, policy = policy)

        val result = engine.enableProtection()

        assertFailure(ProtectionError.ENABLE_FAILED, result)
    }

    @Test
    fun `disable protection verifies that policy became inactive`() {
        val policy = FakeBluetoothPolicy(enabled = true)
        val engine = engine(deviceOwner = true, policy = policy)

        val result = engine.disableProtection()

        assertEquals(ProtectionResult.Success(ProtectionState.READY), result)
        assertTrue(!policy.enabled)
    }

    @Test
    fun `disable protection fails when restriction remains active`() {
        val policy = FakeBluetoothPolicy(
            enabled = true,
            disableChangesState = false,
        )
        val engine = engine(deviceOwner = true, policy = policy)

        val result = engine.disableProtection()

        assertFailure(ProtectionError.DISABLE_NOT_CONFIRMED, result)
    }

    @Test
    fun `disable protection reports platform failure`() {
        val policy = FakeBluetoothPolicy(enabled = true, throwOnDisable = true)
        val engine = engine(deviceOwner = true, policy = policy)

        val result = engine.disableProtection()

        assertFailure(ProtectionError.DISABLE_FAILED, result)
    }

    @Test
    fun `enable and disable return precondition without touching policy when not provisioned`() {
        val policy = FakeBluetoothPolicy(enabled = false)
        val engine = engine(deviceOwner = false, policy = policy)

        assertEquals(
            ProtectionResult.Success(ProtectionState.NOT_PROVISIONED),
            engine.enableProtection(),
        )
        assertEquals(
            ProtectionResult.Success(ProtectionState.NOT_PROVISIONED),
            engine.disableProtection(),
        )
        assertEquals(0, policy.enableCalls)
        assertEquals(0, policy.disableCalls)
    }

    private fun assertFailure(expected: ProtectionError, result: ProtectionResult) {
        assertTrue(result is ProtectionResult.Failure)
        assertEquals(expected, (result as ProtectionResult.Failure).error)
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
        private val disableChangesState: Boolean = true,
        private val throwOnRead: Boolean = false,
        private val throwOnEnable: Boolean = false,
        private val throwOnDisable: Boolean = false,
    ) : BluetoothPolicyController {
        var enableCalls: Int = 0
            private set
        var disableCalls: Int = 0
            private set

        override fun isProtectionEnabled(): Boolean {
            if (throwOnRead) error("read failed")
            return enabled
        }

        override fun enableProtection() {
            enableCalls += 1
            if (throwOnEnable) error("enable failed")
            if (enableChangesState) enabled = true
        }

        override fun disableProtection() {
            disableCalls += 1
            if (throwOnDisable) error("disable failed")
            if (disableChangesState) enabled = false
        }
    }
}
