package com.pulse.bluetoothdisable.cover

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.pulse.bluetoothdisable.R

/** One lifecycle-bound system prompt; no device credentials or auth results are persisted. */
internal class CoverDeviceAuthenticator(private val activity: FragmentActivity) {
    private var completion: ((Boolean) -> Unit)? = null
    private var rejected = false
    private var closed = false
    private val prompt: BiometricPrompt

    init {
        // Construct once in onCreate, so AndroidX attaches its callback to this Activity.
        prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    complete(!rejected && !activity.isFinishing && !activity.isDestroyed)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    complete(false)
                }

                override fun onAuthenticationFailed() {
                    // Keep the attempt occupied until the cancellation's terminal callback.
                    // An old prompt must not cancel or authorize a subsequent attempt.
                    rejected = true
                    prompt.cancelAuthentication()
                }
            },
        )
    }

    fun authenticate(onResult: (Boolean) -> Unit): Boolean {
        if (closed || completion != null) return false
        val authenticators = allowedAuthenticators(Build.VERSION.SDK_INT)
        if (BiometricManager.from(activity).canAuthenticate(authenticators) !=
            BiometricManager.BIOMETRIC_SUCCESS
        ) return false

        completion = onResult
        rejected = false
        return try {
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(activity.getString(R.string.cover_recovery_auth_title))
                    .setDescription(activity.getString(R.string.cover_recovery_auth_description))
                    .setAllowedAuthenticators(authenticators)
                    .build(),
            )
            true
        } catch (_: Exception) {
            completion = null
            prompt.cancelAuthentication()
            false
        }
    }

    fun close() {
        closed = true
        completion = null
        prompt.cancelAuthentication()
    }

    private fun complete(success: Boolean) {
        val callback = completion ?: return
        completion = null
        callback(success)
    }

    companion object {
        internal fun allowedAuthenticators(sdk: Int): Int =
            (if (sdk >= 30) {
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            } else {
                // STRONG | DEVICE_CREDENTIAL is unsupported on API 28–29;
                // AndroidX supplies the system PIN/pattern/password fallback here.
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            }) or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}
