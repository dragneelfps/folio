package com.nooblabs.folio.di

import android.content.Context
import com.nooblabs.folio.data.local.DatabaseHelper
import com.nooblabs.folio.data.repository.SqliteBankAccountRepository
import com.nooblabs.folio.data.repository.SqliteCreditCardRepository
import com.nooblabs.folio.data.repository.SqliteStockInvestmentRepository
import com.nooblabs.folio.data.repository.SqliteTransactionRepository
import com.nooblabs.folio.domain.repository.BankAccountRepository
import com.nooblabs.folio.domain.repository.CreditCardRepository
import com.nooblabs.folio.domain.repository.SettingsRepository
import com.nooblabs.folio.domain.repository.StockInvestmentRepository
import com.nooblabs.folio.domain.repository.TransactionRepository
import com.nooblabs.folio.data.repository.SharedPrefsSettingsRepository
import com.nooblabs.folio.data.repository.LiveStockPriceRepository
import kotlinx.coroutines.CoroutineScope

interface AppContainer {
    val bankAccountRepository: BankAccountRepository
    val stockInvestmentRepository: StockInvestmentRepository
    val liveStockPriceRepository: LiveStockPriceRepository
    val creditCardRepository: CreditCardRepository
    val transactionRepository: TransactionRepository
    val settingsRepository: SettingsRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    // Use the application-level scope so sync jobs are tied to the process, not an ad-hoc scope
    private val appScope: CoroutineScope =
        (context.applicationContext as com.nooblabs.folio.FolioApplication).applicationScope

    override val settingsRepository: SettingsRepository by lazy {
        SharedPrefsSettingsRepository(context)
    }

    private val dbHelper: DatabaseHelper by lazy {
        DatabaseHelper(context)
    }

    override val bankAccountRepository: BankAccountRepository by lazy {
        SqliteBankAccountRepository(dbHelper)
    }

    private val finnhubService: com.nooblabs.folio.data.network.FinnhubService by lazy {
        com.nooblabs.folio.data.network.FinnhubService(settingsRepository)
    }

    override val stockInvestmentRepository: StockInvestmentRepository by lazy {
        SqliteStockInvestmentRepository(dbHelper)
    }

    override val liveStockPriceRepository: LiveStockPriceRepository by lazy {
        LiveStockPriceRepository(dbHelper, finnhubService, appScope)
    }

    override val creditCardRepository: CreditCardRepository by lazy {
        SqliteCreditCardRepository(dbHelper)
    }

    override val transactionRepository: TransactionRepository by lazy {
        SqliteTransactionRepository(dbHelper)
    }
}
