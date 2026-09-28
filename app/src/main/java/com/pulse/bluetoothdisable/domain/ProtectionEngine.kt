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
                message = "Не удалось прочитать системную политику Bluetooth.",
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
                    message = "Android не подтвердил включение системной блокировки Bluetooth.",
                )
            }
        } catch (error: Throwable) {
            ProtectionResult.Failure(
                message = "Не удалось включить системную блокировку Bluetooth.",
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
                    message = "Android не подтвердил снятие системной блокировки Bluetooth.",
                )
            }
        } catch (error: Throwable) {
            ProtectionResult.Failure(
                message = "Не удалось снять системную блокировку Bluetooth.",
                cause = error,
            )
        }
    }
}
