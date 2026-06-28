package com.nooblabs.folio.data.repository

import android.content.Context
import com.nooblabs.folio.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedPrefsSettingsRepository(context: Context) : SettingsRepository {
    private val prefs = context.getSharedPreferences("folio_settings", Context.MODE_PRIVATE)
    
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
        prefs.getString("finnhub_api_key", "") ?: ""
    )

    override val finnhubApiKey: StateFlow<String> = _finnhubApiKey.asStateFlow()

    override fun setFinnhubApiKey(key: String) {
        prefs.edit().putString("finnhub_api_key", key).apply()
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
}
