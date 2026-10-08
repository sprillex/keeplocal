package com.randolph.keeplocal.data.nas

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class NasConfig(
    val hostIp: String = "",
    val shareName: String = "",
    val subfolderPath: String = "",
    val domain: String = "",
    val username: String = "",
    val password: String = ""
) {
    val isValid: Boolean
        get() = hostIp.isNotBlank() && shareName.isNotBlank()
}

class NasCredentialManager(private val context: Context) {

    companion object {
        const val PREF_FILE = "nas_credentials_encrypted"
        const val KEY_HOST_IP = "nas_host_ip"
        const val KEY_SHARE_NAME = "nas_share_name"
        const val KEY_SUBFOLDER = "nas_subfolder_path"
        const val KEY_DOMAIN = "nas_domain"
        const val KEY_USERNAME = "nas_username"
        const val KEY_PASSWORD = "nas_password"
    }

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREF_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        }
    }

    fun getNasConfig(): NasConfig {
        return NasConfig(
            hostIp = prefs.getString(KEY_HOST_IP, "") ?: "",
            shareName = prefs.getString(KEY_SHARE_NAME, "") ?: "",
            subfolderPath = prefs.getString(KEY_SUBFOLDER, "") ?: "",
            domain = prefs.getString(KEY_DOMAIN, "") ?: "",
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            password = prefs.getString(KEY_PASSWORD, "") ?: ""
        )
    }

    fun saveNasConfig(config: NasConfig) {
        prefs.edit()
            .putString(KEY_HOST_IP, config.hostIp)
            .putString(KEY_SHARE_NAME, config.shareName)
            .putString(KEY_SUBFOLDER, config.subfolderPath)
            .putString(KEY_DOMAIN, config.domain)
            .putString(KEY_USERNAME, config.username)
            .putString(KEY_PASSWORD, config.password)
            .apply()
    }

    fun clearNasConfig() {
        prefs.edit().clear().apply()
    }
}
