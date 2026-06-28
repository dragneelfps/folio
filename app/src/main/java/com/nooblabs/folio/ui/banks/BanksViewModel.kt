package com.nooblabs.folio.ui.banks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nooblabs.folio.domain.model.BankAccount
import com.nooblabs.folio.domain.repository.BankAccountRepository
import com.nooblabs.folio.domain.repository.SettingsRepository
import com.nooblabs.folio.domain.util.CurrencyConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BankUiModel(
    val bankAccount: BankAccount,
    val convertedBalance: Double,
    val globalCurrency: String
)

data class BanksUiState(
    val banks: List<BankUiModel> = emptyList(),
    val globalCurrency: String = "USD",
    val error: String? = null
)

class BanksViewModel(
    private val repository: BankAccountRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BanksUiState> = combine(
        repository.getAllBankAccounts(),
        settingsRepository.globalCurrency,
        _error
    ) { banks, globalCurrency, error ->
        val uiModels = banks.map { bank ->
            BankUiModel(
                bankAccount = bank,
                convertedBalance = CurrencyConverter.convert(bank.balance, bank.currency, globalCurrency),
                globalCurrency = globalCurrency
            )
        }
        BanksUiState(banks = uiModels, globalCurrency = globalCurrency, error = error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BanksUiState()
    )

    fun addBank(bankName: String, accountNumber: String, balance: Double, currency: String = "USD") {
        viewModelScope.launch {
            try {
                repository.insertBankAccount(
                    BankAccount(
                        bankName = bankName,
                        accountNumber = accountNumber,
                        balance = balance,
                        currency = currency
                    )
                )
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Failed to add bank account"
            }
        }
    }

    fun updateBank(id: Int, bankName: String, accountNumber: String, balance: Double, currency: String = "USD") {
        viewModelScope.launch {
            try {
                repository.updateBankAccount(
                    BankAccount(
                        id = id,
                        bankName = bankName,
                        accountNumber = accountNumber,
                        balance = balance,
                        currency = currency
                    )
                )
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Failed to update bank account"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun setGlobalCurrency(currency: String) {
        settingsRepository.setGlobalCurrency(currency)
    }
}
