package com.nooblabs.folio.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nooblabs.folio.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedPrefsSettingsRepository(context: Context) : SettingsRepository {
    private val prefs = context.getSharedPreferences("folio_settings", Context.MODE_PRIVATE)

    @Suppress("DEPRECATION")
    private val securePrefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    init {
        migrateApiKeyIfNeeded()
    }

    private fun migrateApiKeyIfNeeded() {
        if (prefs.contains(KEY_FINNHUB_API_KEY)) {
            val legacyKey = prefs.getString(KEY_FINNHUB_API_KEY, "") ?: ""
            if (legacyKey.isNotEmpty() && !securePrefs.contains(KEY_FINNHUB_API_KEY)) {
                securePrefs.edit().putString(KEY_FINNHUB_API_KEY, legacyKey).apply()
            }
            prefs.edit().remove(KEY_FINNHUB_API_KEY).apply()
        }
    }
    
    private val _globalCurrency = MutableStateFlow(
        prefs.getString("global_currency", "USD") ?: "USD"
    )
    
    override val globalCurrency: StateFlow<String> = _globalCurrency.asStateFlow()

    override fun setGlobalCurrency(currencyCode: String) {
        prefs.edit().putString("global_currency", currencyCode).apply()
        _globalCurrency.value = currencyCode
    }

    private val _appTheme = MutableStateFlow(
        prefs.getString("app_theme", "SYSTEM") ?: "SYSTEM"
    )

    override val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    override fun setAppTheme(theme: String) {
        prefs.edit().putString("app_theme", theme).apply()
        _appTheme.value = theme
    }

    private val _finnhubApiKey = MutableStateFlow(
        securePrefs.getString(KEY_FINNHUB_API_KEY, "") ?: ""
    )

    override val finnhubApiKey: StateFlow<String> = _finnhubApiKey.asStateFlow()

    override fun setFinnhubApiKey(key: String) {
        securePrefs.edit().putString(KEY_FINNHUB_API_KEY, key).apply()
        _finnhubApiKey.value = key
    }

    private val _userName = MutableStateFlow(
        prefs.getString("user_name", "") ?: ""
    )

    override val userName: StateFlow<String> = _userName.asStateFlow()

    override fun setUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
        _userName.value = name
    }

    private companion object {
        const val SECURE_PREFS_NAME = "folio_secure_settings"
        const val KEY_FINNHUB_API_KEY = "finnhub_api_key"
    }
}
