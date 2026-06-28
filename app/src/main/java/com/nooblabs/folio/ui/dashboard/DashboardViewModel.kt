package com.nooblabs.folio.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nooblabs.folio.domain.model.Transaction
import com.nooblabs.folio.domain.repository.BankAccountRepository
import com.nooblabs.folio.domain.repository.CreditCardRepository
import com.nooblabs.folio.domain.repository.StockInvestmentRepository
import com.nooblabs.folio.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.nooblabs.folio.domain.repository.SettingsRepository
import com.nooblabs.folio.domain.util.CurrencyConverter

import com.nooblabs.folio.data.repository.LiveStockPriceRepository

data class DashboardUiState(
    val totalBankBalance: Double = 0.0,
    val totalStockValue: Double = 0.0,
    val totalCreditOutstanding: Double = 0.0,
    val netWorth: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val globalCurrency: String = "USD",
    val isLoading: Boolean = true,
    val userName: String = ""
)

class DashboardViewModel(
    bankAccountRepository: BankAccountRepository,
    stockInvestmentRepository: StockInvestmentRepository,
    liveStockPriceRepository: LiveStockPriceRepository,
    creditCardRepository: CreditCardRepository,
    transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val flow1 = combine(
        bankAccountRepository.getAllBankAccounts(),
        stockInvestmentRepository.getAllStockInvestments(),
        liveStockPriceRepository.prices
    ) { banks, stocks, prices ->
        Triple(banks, stocks, prices)
    }

    private val flow2 = combine(
        creditCardRepository.getAllCreditCards(),
        transactionRepository.getAllTransactions(),
        settingsRepository.globalCurrency
    ) { cards, transactions, globalCurrency ->
        Triple(cards, transactions, globalCurrency)
    }

    private val flow3 = settingsRepository.userName

    val uiState: StateFlow<DashboardUiState> = combine(flow1, flow2, flow3) { (banks, stocks, prices), (cards, transactions, globalCurrency), userName ->
        
        val totalBank = banks.sumOf { CurrencyConverter.convert(it.balance, it.currency, globalCurrency) }
        val totalStock = stocks.sumOf { 
            val price = prices[it.tickerSymbol] ?: 0.0
            CurrencyConverter.convert(it.quantity * price, "USD", globalCurrency) 
        }
        val totalCredit = cards.sumOf { CurrencyConverter.convert(it.currentOutstanding, "USD", globalCurrency) }
        
        DashboardUiState(
            totalBankBalance = totalBank,
            totalStockValue = totalStock,
            totalCreditOutstanding = totalCredit,
            netWorth = totalBank + totalStock - totalCredit,
            recentTransactions = transactions.sortedByDescending { it.date }.take(5),
            globalCurrency = globalCurrency,
            isLoading = false,
            userName = userName
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun setGlobalCurrency(currency: String) {
        settingsRepository.setGlobalCurrency(currency)
    }
}
