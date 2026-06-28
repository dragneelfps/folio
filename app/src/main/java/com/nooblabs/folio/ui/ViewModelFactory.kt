package com.nooblabs.folio.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nooblabs.folio.FolioApplication
import com.nooblabs.folio.ui.banks.BanksViewModel
import com.nooblabs.folio.ui.creditcards.CreditCardsViewModel
import com.nooblabs.folio.ui.dashboard.DashboardViewModel
import com.nooblabs.folio.ui.stocks.StocksViewModel
import com.nooblabs.folio.ui.settings.SettingsViewModel
import com.nooblabs.folio.ui.transactions.TransactionsViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            DashboardViewModel(
                folioApplication().container.bankAccountRepository,
                folioApplication().container.stockInvestmentRepository,
                folioApplication().container.liveStockPriceRepository,
                folioApplication().container.creditCardRepository,
                folioApplication().container.transactionRepository,
                folioApplication().container.settingsRepository
            )
        }
        initializer {
            BanksViewModel(
                folioApplication().container.bankAccountRepository,
                folioApplication().container.settingsRepository
            )
        }
        initializer {
            StocksViewModel(
                folioApplication().container.stockInvestmentRepository,
                folioApplication().container.liveStockPriceRepository,
                folioApplication().container.settingsRepository
            )
        }
        initializer {
            CreditCardsViewModel(
                folioApplication().container.creditCardRepository,
                folioApplication().container.settingsRepository
            )
        }
        initializer {
            TransactionsViewModel(
                folioApplication().container.transactionRepository,
                folioApplication().container.bankAccountRepository,
                folioApplication().container.creditCardRepository,
                folioApplication().container.settingsRepository
            )
        }
        initializer {
            SettingsViewModel(
                folioApplication().container.settingsRepository,
                folioApplication().container.bankAccountRepository,
                folioApplication().container.stockInvestmentRepository,
                folioApplication().container.creditCardRepository,
                folioApplication().container.transactionRepository
            )
        }
    }
}

fun CreationExtras.folioApplication(): FolioApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as FolioApplication)
