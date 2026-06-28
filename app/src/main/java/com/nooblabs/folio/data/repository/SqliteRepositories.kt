package com.nooblabs.folio.data.repository

import android.content.ContentValues
import com.nooblabs.folio.data.local.DatabaseHelper
import com.nooblabs.folio.domain.model.BankAccount
import com.nooblabs.folio.domain.model.CreditCard
import com.nooblabs.folio.domain.model.StockInvestment
import com.nooblabs.folio.domain.model.Transaction
import com.nooblabs.folio.domain.repository.BankAccountRepository
import com.nooblabs.folio.domain.repository.CreditCardRepository
import com.nooblabs.folio.domain.repository.StockInvestmentRepository
import com.nooblabs.folio.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import com.nooblabs.folio.data.network.FinnhubService

class SqliteBankAccountRepository(private val dbHelper: DatabaseHelper) : BankAccountRepository {
    private val _accounts = MutableStateFlow<List<BankAccount>>(emptyList())

    init {
        refresh()
    }

    private fun refresh() {
        val list = mutableListOf<BankAccount>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM bank_accounts", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(BankAccount(
                    id = it.getInt(it.getColumnIndexOrThrow("id")),
                    bankName = it.getString(it.getColumnIndexOrThrow("bankName")),
                    accountNumber = it.getString(it.getColumnIndexOrThrow("accountNumber")),
                    balance = it.getDouble(it.getColumnIndexOrThrow("balance")),
                    currency = it.getString(it.getColumnIndexOrThrow("currency"))
                ))
            }
        }
        _accounts.value = list
    }

    override fun getAllBankAccounts(): Flow<List<BankAccount>> = _accounts.asStateFlow()

    override suspend fun insertBankAccount(account: BankAccount) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("bankName", account.bankName)
            put("accountNumber", account.accountNumber)
            put("balance", account.balance)
            put("currency", account.currency)
        }
        val rowId = db.insert("bank_accounts", null, cv)
        check(rowId != -1L) { "Failed to insert bank account" }
        refresh()
    }

    override suspend fun updateBankAccount(account: BankAccount) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("bankName", account.bankName)
            put("accountNumber", account.accountNumber)
            put("balance", account.balance)
            put("currency", account.currency)
        }
        val rowsAffected = db.update("bank_accounts", cv, "id = ?", arrayOf(account.id.toString()))
        check(rowsAffected > 0) { "Failed to update bank account: ID ${account.id} not found" }
        refresh()
    }

    override suspend fun deleteBankAccount(account: BankAccount) {
        val db = dbHelper.writableDatabase
        db.delete("bank_accounts", "id = ?", arrayOf(account.id.toString()))
        refresh()
    }

    override suspend fun deleteAll() {
        val db = dbHelper.writableDatabase
        db.delete("bank_accounts", null, null)
        refresh()
    }
}

class SqliteStockInvestmentRepository(
    private val dbHelper: DatabaseHelper
) : StockInvestmentRepository {
    private val _stocks = MutableStateFlow<List<StockInvestment>>(emptyList())

    init {
        refresh()
    }

    private fun refresh() {
        val list = mutableListOf<StockInvestment>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM stock_investments", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(StockInvestment(
                    id = it.getInt(it.getColumnIndexOrThrow("id")),
                    tickerSymbol = it.getString(it.getColumnIndexOrThrow("tickerSymbol")),
                    quantity = it.getDouble(it.getColumnIndexOrThrow("quantity")),
                    averageBuyPrice = it.getDouble(it.getColumnIndexOrThrow("averageBuyPrice")),
                    purchaseDate = it.getLong(it.getColumnIndexOrThrow("purchaseDate"))
                ))
            }
        }
        _stocks.value = list
    }

    override fun getAllStockInvestments(): Flow<List<StockInvestment>> = _stocks.asStateFlow()

    override suspend fun insertStockInvestment(investment: StockInvestment) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("tickerSymbol", investment.tickerSymbol)
            put("quantity", investment.quantity)
            put("averageBuyPrice", investment.averageBuyPrice)
            put("purchaseDate", investment.purchaseDate)
        }
        val rowId = db.insert("stock_investments", null, cv)
        check(rowId != -1L) { "Failed to insert stock investment" }
        refresh()
    }

    override suspend fun updateStockInvestment(investment: StockInvestment) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("tickerSymbol", investment.tickerSymbol)
            put("quantity", investment.quantity)
            put("averageBuyPrice", investment.averageBuyPrice)
            put("purchaseDate", investment.purchaseDate)
        }
        val rowsAffected = db.update("stock_investments", cv, "id = ?", arrayOf(investment.id.toString()))
        check(rowsAffected > 0) { "Failed to update stock investment: ID ${investment.id} not found" }
        refresh()
    }

    override suspend fun deleteStockInvestment(investment: StockInvestment) {
        val db = dbHelper.writableDatabase
        db.delete("stock_investments", "id = ?", arrayOf(investment.id.toString()))
        refresh()
    }

    override suspend fun deleteAll() {
        val db = dbHelper.writableDatabase
        db.delete("stock_investments", null, null)
        refresh()
    }
}

class SqliteCreditCardRepository(private val dbHelper: DatabaseHelper) : CreditCardRepository {
    private val _cards = MutableStateFlow<List<CreditCard>>(emptyList())

    init {
        refresh()
    }

    private fun refresh() {
        val list = mutableListOf<CreditCard>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM credit_cards", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(CreditCard(
                    id = it.getInt(it.getColumnIndexOrThrow("id")),
                    cardName = it.getString(it.getColumnIndexOrThrow("cardName")),
                    cardNumber = it.getString(it.getColumnIndexOrThrow("cardNumber")),
                    creditLimit = it.getDouble(it.getColumnIndexOrThrow("creditLimit")),
                    currentOutstanding = it.getDouble(it.getColumnIndexOrThrow("currentOutstanding")),
                    dueDate = it.getLong(it.getColumnIndexOrThrow("dueDate")),
                    expiry = it.getString(it.getColumnIndexOrThrow("expiry"))
                ))
            }
        }
        _cards.value = list
    }

    override fun getAllCreditCards(): Flow<List<CreditCard>> = _cards.asStateFlow()

    override suspend fun insertCreditCard(card: CreditCard) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("cardName", card.cardName)
            put("cardNumber", card.cardNumber)
            put("creditLimit", card.creditLimit)
            put("currentOutstanding", card.currentOutstanding)
            put("dueDate", card.dueDate)
            put("expiry", card.expiry)
        }
        val rowId = db.insert("credit_cards", null, cv)
        check(rowId != -1L) { "Failed to insert credit card" }
        refresh()
    }

    override suspend fun updateCreditCard(card: CreditCard) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("cardName", card.cardName)
            put("cardNumber", card.cardNumber)
            put("creditLimit", card.creditLimit)
            put("currentOutstanding", card.currentOutstanding)
            put("dueDate", card.dueDate)
            put("expiry", card.expiry)
        }
        val rowsAffected = db.update("credit_cards", cv, "id = ?", arrayOf(card.id.toString()))
        check(rowsAffected > 0) { "Failed to update credit card: ID ${card.id} not found" }
        refresh()
    }

    override suspend fun deleteCreditCard(card: CreditCard) {
        val db = dbHelper.writableDatabase
        db.delete("credit_cards", "id = ?", arrayOf(card.id.toString()))
        refresh()
    }

    override suspend fun deleteAll() {
        val db = dbHelper.writableDatabase
        db.delete("credit_cards", null, null)
        refresh()
    }
}

class SqliteTransactionRepository(private val dbHelper: DatabaseHelper) : TransactionRepository {
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())

    init {
        refresh()
    }

    private fun refresh() {
        val list = mutableListOf<Transaction>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM transactions", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(Transaction(
                    id = it.getInt(it.getColumnIndexOrThrow("id")),
                    amount = it.getDouble(it.getColumnIndexOrThrow("amount")),
                    date = it.getLong(it.getColumnIndexOrThrow("date")),
                    description = it.getString(it.getColumnIndexOrThrow("description")),
                    type = it.getString(it.getColumnIndexOrThrow("type")),
                    category = it.getString(it.getColumnIndexOrThrow("category")),
                    sourceId = it.getInt(it.getColumnIndexOrThrow("sourceId")),
                    sourceType = it.getString(it.getColumnIndexOrThrow("sourceType")),
                    currency = it.getString(it.getColumnIndexOrThrow("currency"))
                ))
            }
        }
        _transactions.value = list
    }

    override fun getAllTransactions(): Flow<List<Transaction>> = _transactions.asStateFlow()

    override fun getTransactionsBySource(sourceId: Int, sourceType: String): Flow<List<Transaction>> {
        // Return a filtered view of the live _transactions flow so callers receive updates
        return _transactions.asStateFlow().map { list ->
            list.filter { it.sourceId == sourceId && it.sourceType == sourceType }
        }
    }

    override suspend fun insertTransaction(transaction: Transaction) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("amount", transaction.amount)
            put("date", transaction.date)
            put("description", transaction.description)
            put("type", transaction.type)
            put("category", transaction.category)
            put("sourceId", transaction.sourceId)
            put("sourceType", transaction.sourceType)
            put("currency", transaction.currency)
        }
        val rowId = db.insert("transactions", null, cv)
        check(rowId != -1L) { "Failed to insert transaction" }
        refresh()
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("amount", transaction.amount)
            put("date", transaction.date)
            put("description", transaction.description)
            put("type", transaction.type)
            put("category", transaction.category)
            put("sourceId", transaction.sourceId)
            put("sourceType", transaction.sourceType)
            put("currency", transaction.currency)
        }
        val rowsAffected = db.update("transactions", cv, "id = ?", arrayOf(transaction.id.toString()))
        check(rowsAffected > 0) { "Failed to update transaction: ID ${transaction.id} not found" }
        refresh()
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        val db = dbHelper.writableDatabase
        db.delete("transactions", "id = ?", arrayOf(transaction.id.toString()))
        refresh()
    }

    override suspend fun deleteAll() {
        val db = dbHelper.writableDatabase
        db.delete("transactions", null, null)
        refresh()
    }
}
