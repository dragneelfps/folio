package com.nooblabs.folio.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nooblabs.folio.domain.model.BankAccount
import com.nooblabs.folio.domain.model.CreditCard
import com.nooblabs.folio.domain.model.Transaction
import com.nooblabs.folio.domain.repository.BankAccountRepository
import com.nooblabs.folio.domain.repository.CreditCardRepository
import com.nooblabs.folio.domain.repository.SettingsRepository
import com.nooblabs.folio.domain.repository.TransactionRepository
import com.nooblabs.folio.domain.util.CurrencyConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionUiModel(
    val transaction: Transaction,
    val sourceName: String,
    val convertedAmount: Double,
    val globalCurrency: String
)

data class TransactionSource(
    val id: Int,
    val name: String,
    val type: String, // "BANK" or "CARD"
    val currency: String
)

data class TransactionsUiState(
    val transactions: List<TransactionUiModel> = emptyList(),
    val sources: List<TransactionSource> = emptyList(),
    val globalCurrency: String = "USD",
    val error: String? = null
)

class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val bankAccountRepository: BankAccountRepository,
    private val creditCardRepository: CreditCardRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.getAllTransactions(),
        bankAccountRepository.getAllBankAccounts(),
        creditCardRepository.getAllCreditCards(),
        settingsRepository.globalCurrency,
        _error
    ) { transactions, banks, cards, globalCurrency, error ->
        val sourcesList = banks.map { TransactionSource(it.id, it.bankName, "BANK", it.currency) } +
                cards.map { TransactionSource(it.id, it.cardName, "CARD", "USD") }

        val mappedTx = transactions.map { tx ->
            val sourceName = sourcesList.find { it.id == tx.sourceId && it.type == tx.sourceType }?.name ?: "Unknown"
            val convertedAmount = CurrencyConverter.convert(tx.amount, tx.currency, globalCurrency)
            TransactionUiModel(tx, sourceName, convertedAmount, globalCurrency)
        }
        TransactionsUiState(
            transactions = mappedTx.sortedByDescending { it.transaction.date },
            sources = sourcesList,
            globalCurrency = globalCurrency,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun addTransaction(
        amount: Double,
        date: Long,
        description: String,
        type: String,
        category: String,
        sourceId: Int,
        sourceType: String,
        currency: String
    ) {
        viewModelScope.launch {
            try {
                // 1. Insert transaction
                val newTx = Transaction(
                    amount = amount,
                    date = date,
                    description = description,
                    type = type,
                    category = category,
                    sourceId = sourceId,
                    sourceType = sourceType,
                    currency = currency
                )
                transactionRepository.insertTransaction(newTx)

                // 2. Adjust account balances
                adjustAccountBalance(sourceId, sourceType, type, amount, currency)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Failed to add transaction"
            }
        }
    }

    fun updateTransaction(
        id: Int,
        amount: Double,
        date: Long,
        description: String,
        type: String,
        category: String,
        sourceId: Int,
        sourceType: String,
        currency: String,
        oldTx: Transaction
    ) {
        viewModelScope.launch {
            try {
                // 1. Revert effect of the old transaction
                adjustAccountBalance(
                    sourceId = oldTx.sourceId,
                    sourceType = oldTx.sourceType,
                    type = oldTx.type,
                    amount = -oldTx.amount, // Pass negative to reverse
                    txCurrency = oldTx.currency
                )

                // 2. Apply effect of the new transaction
                adjustAccountBalance(
                    sourceId = sourceId,
                    sourceType = sourceType,
                    type = type,
                    amount = amount,
                    txCurrency = currency
                )

                // 3. Update in database
                val updatedTx = Transaction(
                    id = id,
                    amount = amount,
                    date = date,
                    description = description,
                    type = type,
                    category = category,
                    sourceId = sourceId,
                    sourceType = sourceType,
                    currency = currency
                )
                transactionRepository.updateTransaction(updatedTx)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Failed to update transaction"
            }
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            try {
                // 1. Revert effect of the transaction
                adjustAccountBalance(
                    sourceId = tx.sourceId,
                    sourceType = tx.sourceType,
                    type = tx.type,
                    amount = -tx.amount, // Pass negative to reverse
                    txCurrency = tx.currency
                )

                // 2. Delete from database
                transactionRepository.deleteTransaction(tx)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Failed to delete transaction"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private suspend fun adjustAccountBalance(
        sourceId: Int,
        sourceType: String,
        type: String,
        amount: Double,
        txCurrency: String
    ) {
        if (sourceType == "BANK") {
            val bankList = bankAccountRepository.getAllBankAccounts().first()
            val bank = bankList.find { it.id == sourceId } ?: return
            
            // Convert transaction amount to bank currency before adjustment
            val amountInBankCurrency = CurrencyConverter.convert(amount, txCurrency, bank.currency)

            // For Bank Account: CREDIT adds balance, DEBIT subtracts balance
            val balanceChange = if (type == "CREDIT") amountInBankCurrency else -amountInBankCurrency
            val updatedBank = bank.copy(balance = bank.balance + balanceChange)
            bankAccountRepository.updateBankAccount(updatedBank)
        } else if (sourceType == "CARD") {
            val cardList = creditCardRepository.getAllCreditCards().first()
            val card = cardList.find { it.id == sourceId } ?: return

            // Convert transaction amount to credit card currency (USD) before adjustment
            val amountInCardCurrency = CurrencyConverter.convert(amount, txCurrency, "USD")

            // For Credit Card: DEBIT (purchase) adds to outstanding, CREDIT (payment) subtracts from outstanding
            val outstandingChange = if (type == "DEBIT") amountInCardCurrency else -amountInCardCurrency
            val updatedCard = card.copy(currentOutstanding = card.currentOutstanding + outstandingChange)
            creditCardRepository.updateCreditCard(updatedCard)
        }
    }

    fun setGlobalCurrency(currency: String) {
        settingsRepository.setGlobalCurrency(currency)
    }
}
