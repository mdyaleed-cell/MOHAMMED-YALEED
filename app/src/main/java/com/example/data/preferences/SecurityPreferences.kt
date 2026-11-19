package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.securityDataStore by preferencesDataStore(name = "hisab_khata_security_prefs")

data class SecuritySettings(
    val pinLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val pinHash: String = "",
    val darkThemeOverride: Boolean? = null
)

class SecurityPreferences(private val context: Context) {
    private object Keys {
        val PIN_LOCK_ENABLED = booleanPreferencesKey("pin_lock_enabled")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val DARK_THEME_MODE = stringPreferencesKey("dark_theme_mode") // "SYSTEM", "LIGHT", "DARK"
    }

    val settingsFlow: Flow<SecuritySettings> = context.securityDataStore.data.map { prefs ->
        val darkPref = when (prefs[Keys.DARK_THEME_MODE]) {
            "LIGHT" -> false
            "DARK" -> true
            else -> null
        }
        SecuritySettings(
            pinLockEnabled = prefs[Keys.PIN_LOCK_ENABLED] ?: false,
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            pinHash = prefs[Keys.PIN_HASH] ?: "",
            darkThemeOverride = darkPref
        )
    }

    suspend fun setPin(pin: String) {
        val hashed = hashPin(pin)
        context.securityDataStore.edit { prefs ->
            prefs[Keys.PIN_HASH] = hashed
            prefs[Keys.PIN_LOCK_ENABLED] = true
        }
    }

    suspend fun disablePinLock() {
        context.securityDataStore.edit { prefs ->
            prefs[Keys.PIN_LOCK_ENABLED] = false
            prefs[Keys.BIOMETRIC_ENABLED] = false
            prefs.remove(Keys.PIN_HASH)
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.securityDataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setDarkThemeOverride(dark: Boolean?) {
        context.securityDataStore.edit { prefs ->
            prefs[Keys.DARK_THEME_MODE] = when (dark) {
                true -> "DARK"
                false -> "LIGHT"
                null -> "SYSTEM"
            }
        }
    }

    fun verifyPin(pin: String, storedHash: String): Boolean {
        return storedHash.isNotEmpty() && hashPin(pin) == storedHash
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(("hisab_khata_salt_$pin").toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
