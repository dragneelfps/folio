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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.nooblabs.folio.domain.model.BankAccount
import com.nooblabs.folio.domain.model.CreditCard
import com.nooblabs.folio.domain.model.StockInvestment
import com.nooblabs.folio.domain.model.Transaction

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

    fun generateMockData() {
        viewModelScope.launch {
            val baseCurrency = uiState.value.globalCurrency

            // 1. Create HDFC Bank Account
            val bank = BankAccount(
                bankName = "HDFC",
                accountNumber = "**** **** 4321",
                balance = (15000..75000).random().toDouble(),
                currency = baseCurrency
            )
            bankAccountRepository.insertBankAccount(bank)

            // Retrieve generated bank to get ID
            val insertedBank = bankAccountRepository.getAllBankAccounts().first()
                .firstOrNull { it.bankName == "HDFC" && it.accountNumber == "**** **** 4321" }

            // 2. Create IDFC Credit Card
            val card = CreditCard(
                cardName = "IDFC",
                cardNumber = "**** **** **** 5678",
                creditLimit = 150000.0,
                currentOutstanding = (8000..45000).random().toDouble(),
                dueDate = System.currentTimeMillis() + 15L * 24 * 60 * 60 * 1000,
                expiry = "08/30"
            )
            creditCardRepository.insertCreditCard(card)

            // Retrieve generated card to get ID
            val insertedCard = creditCardRepository.getAllCreditCards().first()
                .firstOrNull { it.cardName == "IDFC" && it.cardNumber == "**** **** **** 5678" }

            // 3. Create Stock Investments (NVDA, AAPL)
            val nvda = StockInvestment(
                tickerSymbol = "NVDA",
                quantity = (15..60).random().toDouble(),
                averageBuyPrice = (115..135).random().toDouble(),
                purchaseDate = System.currentTimeMillis() - 8L * 24 * 60 * 60 * 1000
            )
            stockInvestmentRepository.insertStockInvestment(nvda)

            val aapl = StockInvestment(
                tickerSymbol = "AAPL",
                quantity = (10..40).random().toDouble(),
                averageBuyPrice = (175..195).random().toDouble(),
                purchaseDate = System.currentTimeMillis() - 15L * 24 * 60 * 60 * 1000
            )
            stockInvestmentRepository.insertStockInvestment(aapl)

            // 4. Create sample transactions
            if (insertedBank != null) {
                transactionRepository.insertTransaction(
                    Transaction(
                        amount = 45000.0,
                        date = System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000,
                        description = "Monthly Salary",
                        type = "CREDIT",
                        category = "Salary",
                        sourceId = insertedBank.id,
                        sourceType = "BANK",
                        currency = baseCurrency
                    )
                )
                transactionRepository.insertTransaction(
                    Transaction(
                        amount = 1500.0,
                        date = System.currentTimeMillis() - 1L * 24 * 60 * 60 * 1000,
                        description = "Supermarket Grocery",
                        type = "DEBIT",
                        category = "Groceries",
                        sourceId = insertedBank.id,
                        sourceType = "BANK",
                        currency = baseCurrency
                    )
                )
            }

            if (insertedCard != null) {
                transactionRepository.insertTransaction(
                    Transaction(
                        amount = 3200.0,
                        date = System.currentTimeMillis() - 2L * 24 * 60 * 60 * 1000,
                        description = "Online Shopping",
                        type = "DEBIT",
                        category = "Shopping",
                        sourceId = insertedCard.id,
                        sourceType = "CARD",
                        currency = baseCurrency
                    )
                )
                transactionRepository.insertTransaction(
                    Transaction(
                        amount = 450.0,
                        date = System.currentTimeMillis() - 4L * 24 * 60 * 60 * 1000,
                        description = "Coffee & Snacks",
                        type = "DEBIT",
                        category = "Food & Dining",
                        sourceId = insertedCard.id,
                        sourceType = "CARD",
                        currency = baseCurrency
                    )
                )
            }
        }
    }
}
