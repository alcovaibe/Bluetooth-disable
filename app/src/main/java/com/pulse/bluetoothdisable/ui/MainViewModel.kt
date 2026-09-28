package com.pulse.bluetoothdisable.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.pulse.bluetoothdisable.admin.DeviceOwnerManager
import com.pulse.bluetoothdisable.diagnostics.CapabilityDetector
import com.pulse.bluetoothdisable.domain.ProtectionEngine
import com.pulse.bluetoothdisable.domain.ProtectionError
import com.pulse.bluetoothdisable.domain.ProtectionResult
import com.pulse.bluetoothdisable.domain.ProtectionState
import com.pulse.bluetoothdisable.policy.AndroidBluetoothPolicyController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProtectionUiState(
    val state: ProtectionState = ProtectionState.NOT_PROVISIONED,
    val error: ProtectionError? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = ProtectionEngine(
        deviceOwnerStatus = DeviceOwnerManager(application),
        bluetoothCapability = CapabilityDetector(application),
        bluetoothPolicy = AndroidBluetoothPolicyController(application),
    )

    private val _uiState = MutableStateFlow(ProtectionUiState())
    val uiState: StateFlow<ProtectionUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        applyResult(engine.currentState())
    }

    fun enableProtection() {
        _uiState.value = ProtectionUiState(state = ProtectionState.ENABLING)
        applyResult(engine.enableProtection())
    }

    fun disableProtection() {
        _uiState.value = ProtectionUiState(state = ProtectionState.DISABLING)
        applyResult(engine.disableProtection())
    }

    private fun applyResult(result: ProtectionResult) {
        _uiState.value = when (result) {
            is ProtectionResult.Success -> ProtectionUiState(state = result.state)
            is ProtectionResult.Failure -> ProtectionUiState(
                state = ProtectionState.ERROR,
                error = result.error,
            )
        }
    }
}
