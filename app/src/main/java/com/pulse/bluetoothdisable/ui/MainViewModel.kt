package com.pulse.bluetoothdisable.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.bluetoothdisable.admin.DeviceOwnerManager
import com.pulse.bluetoothdisable.diagnostics.CapabilityDetector
import com.pulse.bluetoothdisable.domain.ProtectionEngine
import com.pulse.bluetoothdisable.domain.ProtectionError
import com.pulse.bluetoothdisable.domain.ProtectionResult
import com.pulse.bluetoothdisable.domain.ProtectionState
import com.pulse.bluetoothdisable.policy.AndroidBluetoothPolicyController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private var operationJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        launchEngineOperation(engine::currentState)
    }

    fun enableProtection() {
        _uiState.value = ProtectionUiState(state = ProtectionState.ENABLING)
        launchEngineOperation(engine::enableProtection)
    }

    fun disableProtection() {
        _uiState.value = ProtectionUiState(state = ProtectionState.DISABLING)
        launchEngineOperation(engine::disableProtection)
    }

    private fun launchEngineOperation(operation: () -> ProtectionResult) {
        // DevicePolicyManager and Bluetooth adapter calls are system-service work and must not
        // block Compose's main/UI thread. Only the newest requested operation is allowed to
        // publish a result; a stale cancelled operation may still finish in the platform, but
        // it cannot overwrite the state produced by the newer request.
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                operation()
            }
            applyResult(result)
        }
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
