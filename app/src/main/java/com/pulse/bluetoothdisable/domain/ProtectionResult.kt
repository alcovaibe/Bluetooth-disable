package com.pulse.bluetoothdisable.domain

sealed interface ProtectionResult {
    data class Success(val state: ProtectionState) : ProtectionResult

    data class Failure(
        val error: ProtectionError,
        val cause: Throwable? = null,
    ) : ProtectionResult
}
