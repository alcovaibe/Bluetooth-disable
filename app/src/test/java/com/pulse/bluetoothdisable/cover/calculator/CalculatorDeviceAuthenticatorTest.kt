package com.pulse.bluetoothdisable.cover.calculator

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorDeviceAuthenticatorTest {
    @Test
    fun oldAndroidVersionsUseSupportedBiometricAndSystemCredentialCombination() {
        for (sdk in 26..29) {
            assertEquals(
                BIOMETRIC_WEAK or DEVICE_CREDENTIAL,
                CalculatorDeviceAuthenticator.allowedAuthenticators(sdk),
            )
        }
    }

    @Test
    fun modernAndroidVersionsAllowStrongBiometricsAndSystemCredentials() {
        for (sdk in 30..36) {
            assertEquals(
                BIOMETRIC_STRONG or DEVICE_CREDENTIAL,
                CalculatorDeviceAuthenticator.allowedAuthenticators(sdk),
            )
        }
    }
}
