package com.pulse.bluetoothdisable.domain

class ProtectionEngine(
    private val deviceOwnerStatus: DeviceOwnerStatus,
    private val bluetoothCapability: BluetoothCapability,
    private val bluetoothPolicy: BluetoothPolicyController,
) {
    fun currentState(): ProtectionResult {
        if (!deviceOwnerStatus.isDeviceOwner()) {
            return ProtectionResult.Success(ProtectionState.NOT_PROVISIONED)
        }

        if (!bluetoothCapability.isBluetoothSupported()) {
            return ProtectionResult.Success(ProtectionState.UNSUPPORTED)
        }

        return try {
            val state = if (bluetoothPolicy.isProtectionEnabled()) {
                ProtectionState.PROTECTED
            } else {
                ProtectionState.READY
            }
            ProtectionResult.Success(state)
        } catch (error: Throwable) {
            ProtectionResult.Failure(
                error = ProtectionError.READ_POLICY,
                cause = error,
            )
        }
    }

    fun enableProtection(): ProtectionResult {
        val precondition = currentState()
        if (precondition !is ProtectionResult.Success) return precondition
        if (precondition.state == ProtectionState.NOT_PROVISIONED ||
            precondition.state == ProtectionState.UNSUPPORTED
        ) {
            return precondition
        }

        return try {
            bluetoothPolicy.enableProtection()
            if (bluetoothPolicy.isProtectionEnabled()) {
                ProtectionResult.Success(ProtectionState.PROTECTED)
            } else {
                ProtectionResult.Failure(
                    error = ProtectionError.ENABLE_NOT_CONFIRMED,
                )
            }
        } catch (error: Throwable) {
            ProtectionResult.Failure(
                error = ProtectionError.ENABLE_POLICY,
                cause = error,
            )
        }
    }

    fun disableProtection(): ProtectionResult {
        val precondition = currentState()
        if (precondition !is ProtectionResult.Success) return precondition
        if (precondition.state == ProtectionState.NOT_PROVISIONED ||
            precondition.state == ProtectionState.UNSUPPORTED
        ) {
            return precondition
        }

        return try {
            bluetoothPolicy.disableProtection()
            if (!bluetoothPolicy.isProtectionEnabled()) {
                ProtectionResult.Success(ProtectionState.READY)
            } else {
                ProtectionResult.Failure(
                    error = ProtectionError.DISABLE_NOT_CONFIRMED,
                )
            }
        } catch (error: Throwable) {
            ProtectionResult.Failure(
                error = ProtectionError.DISABLE_POLICY,
                cause = error,
            )
        }
    }
}
