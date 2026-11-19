package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

object BiometricHelper {
    fun isBiometricAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bm = context.getSystemService(BiometricManager::class.java) ?: return false
            val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            } else {
                @Suppress("DEPRECATION")
                bm.canAuthenticate()
            }
            return res == BiometricManager.BIOMETRIC_SUCCESS
        }
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    }

    fun authenticate(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onError("এই ডিভাইসে বায়োমেট্রিক সমর্থিত নয়")
            return
        }
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt.Builder(context)
            .setTitle("হিসাব খাতা আনলক করুন")
            .setSubtitle("আপনার ফিঙ্গারপ্রিন্ট বা বায়োমেট্রিক দিয়ে প্রবেশ করুন")
            .setNegativeButton("পিন ব্যবহার করুন", executor) { _, _ -> }
            .build()

        val cancellationSignal = CancellationSignal()
        prompt.authenticate(
            cancellationSignal,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED
                    ) {
                        onError(errString?.toString() ?: "বায়োমেট্রিক যাচাই ব্যর্থ হয়েছে")
                    }
                }
            }
        )
    }
}
