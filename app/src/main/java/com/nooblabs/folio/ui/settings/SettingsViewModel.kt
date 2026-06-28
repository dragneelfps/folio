package com.nooblabs.folio.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nooblabs.folio.domain.repository.BankAccountRepository
import com.nooblabs.folio.domain.repository.CreditCardRepository
import com.nooblabs.folio.domain.repository.SettingsRepository
import com.nooblabs.folio.domain.repository.StockInvestmentRepository
import com.nooblabs.folio.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val currentTheme: String = "SYSTEM", // "LIGHT", "DARK", "SYSTEM"
    val globalCurrency: String = "USD",
    val finnhubApiKey: String = "",
    val userName: String = ""
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val bankAccountRepository: BankAccountRepository,
    private val stockInvestmentRepository: StockInvestmentRepository,
    private val creditCardRepository: CreditCardRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            settingsRepository.appTheme,
            settingsRepository.globalCurrency,
            settingsRepository.finnhubApiKey
        ) { theme, currency, apiKey -> Triple(theme, currency, apiKey) },
        settingsRepository.userName
    ) { (theme, currency, apiKey), name ->
        SettingsUiState(
            currentTheme = theme,
            globalCurrency = currency,
            finnhubApiKey = apiKey,
            userName = name
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsRepository.setAppTheme(theme)
        }
    }

    fun setGlobalCurrency(currency: String) {
        viewModelScope.launch {
            settingsRepository.setGlobalCurrency(currency)
        }
    }

    fun setFinnhubApiKey(key: String) {
        viewModelScope.launch {
            settingsRepository.setFinnhubApiKey(key)
        }
    }

    fun updateUserName(name: String) {
        settingsRepository.setUserName(name)
    }

    fun resetAllData() {
        viewModelScope.launch {
            // Delete all entries from SQLite database
            transactionRepository.deleteAll()
            bankAccountRepository.deleteAll()
            stockInvestmentRepository.deleteAll()
            creditCardRepository.deleteAll()

            // Reset Settings to defaults
            settingsRepository.setAppTheme("SYSTEM")
            settingsRepository.setGlobalCurrency("USD")
            settingsRepository.setFinnhubApiKey("")
            settingsRepository.setUserName("")
        }
    }
}
