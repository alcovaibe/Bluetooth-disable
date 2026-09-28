package com.pulse.bluetoothdisable.domain

enum class ProtectionState {
    NOT_PROVISIONED,
    READY,
    ENABLING,
    PROTECTED,
    DISABLING,
    ERROR,
    UNSUPPORTED,
}
