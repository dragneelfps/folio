package com.nooblabs.folio.domain.repository

import com.nooblabs.folio.domain.model.BankAccount
import com.nooblabs.folio.domain.model.CreditCard
import com.nooblabs.folio.domain.model.StockInvestment
import com.nooblabs.folio.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface BankAccountRepository {
    fun getAllBankAccounts(): Flow<List<BankAccount>>
    suspend fun insertBankAccount(account: BankAccount)
    suspend fun updateBankAccount(account: BankAccount)
    suspend fun deleteBankAccount(account: BankAccount)
    suspend fun deleteAll()
}

interface StockInvestmentRepository {
    fun getAllStockInvestments(): Flow<List<StockInvestment>>
    suspend fun insertStockInvestment(investment: StockInvestment)
    suspend fun updateStockInvestment(investment: StockInvestment)
    suspend fun deleteStockInvestment(investment: StockInvestment)
    suspend fun deleteAll()
}

interface CreditCardRepository {
    fun getAllCreditCards(): Flow<List<CreditCard>>
    suspend fun insertCreditCard(card: CreditCard)
    suspend fun updateCreditCard(card: CreditCard)
    suspend fun deleteCreditCard(card: CreditCard)
    suspend fun deleteAll()
}

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsBySource(sourceId: Int, sourceType: String): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun deleteAll()
}
