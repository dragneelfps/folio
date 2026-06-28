package com.nooblabs.folio.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val globalCurrency: StateFlow<String>
    fun setGlobalCurrency(currencyCode: String)
    val appTheme: StateFlow<String> // "LIGHT", "DARK", "SYSTEM"
    fun setAppTheme(theme: String)
    val finnhubApiKey: StateFlow<String>
    fun setFinnhubApiKey(key: String)
    val userName: StateFlow<String>
    fun setUserName(name: String)
}
