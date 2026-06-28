package com.nooblabs.folio.ui.assets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nooblabs.folio.domain.repository.SettingsRepository
import com.nooblabs.folio.domain.util.CurrencyConverter
import com.nooblabs.folio.ui.AppViewModelProvider
import com.nooblabs.folio.ui.banks.BanksScreen
import com.nooblabs.folio.ui.banks.BanksViewModel
import com.nooblabs.folio.ui.creditcards.CreditCardsScreen
import com.nooblabs.folio.ui.creditcards.CreditCardsViewModel
import com.nooblabs.folio.ui.stocks.StocksScreen
import com.nooblabs.folio.ui.stocks.StocksViewModel
import kotlinx.coroutines.launch

private data class AssetTab(val label: String, val icon: ImageVector)

private val assetTabs = listOf(
    AssetTab("Banks", Icons.Filled.AccountBalance),
    AssetTab("Stocks", Icons.Filled.ShowChart),
    AssetTab("Cards", Icons.Filled.CreditCard)
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(
    initialTabIndex: Int,
    settingsRepository: SettingsRepository,
    onNavigateToSettings: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialTabIndex,
        pageCount = { assetTabs.size }
    )
    val scope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(initialTabIndex) {
        if (pagerState.currentPage != initialTabIndex) {
            pagerState.scrollToPage(initialTabIndex)
        }
    }

    val globalCurrency by settingsRepository.globalCurrency.collectAsState()
    var dropdownExpanded by remember { mutableStateOf(false) }

    // We instantiate the viewModels here to coordinate top bar actions if needed
    val banksViewModel: BanksViewModel = viewModel(
        key = "banks",
        factory = AppViewModelProvider.Factory
    )
    val stocksViewModel: StocksViewModel = viewModel(
        key = "stocks",
        factory = AppViewModelProvider.Factory
    )
    val creditCardsViewModel: CreditCardsViewModel = viewModel(
        key = "cards",
        factory = AppViewModelProvider.Factory
    )

    val stocksUiState by stocksViewModel.uiState.collectAsState()

    var showBankSheet by remember { mutableStateOf(false) }
    var editingBank by remember { mutableStateOf<com.nooblabs.folio.domain.model.BankAccount?>(null) }

    var showStockSheet by remember { mutableStateOf(false) }
    var editingStock by remember { mutableStateOf<com.nooblabs.folio.domain.model.StockInvestment?>(null) }

    var showCardSheet by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<com.nooblabs.folio.domain.model.CreditCard?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assets", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    // Show stock refresh icon only when on the Stocks tab (page index 1)
                    if (pagerState.currentPage == 1) {
                        if (stocksUiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            IconButton(onClick = { stocksViewModel.syncPrices() }) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Refresh Prices",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Global currency selector dropdown
                    Box(contentAlignment = Alignment.Center) {
                        TextButton(onClick = { dropdownExpanded = true }) {
                            Text("$globalCurrency ▼", color = MaterialTheme.colorScheme.onBackground)
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            CurrencyConverter.supportedCurrencies.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text(currency) },
                                    onClick = {
                                        settingsRepository.setGlobalCurrency(currency)
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (pagerState.currentPage) {
                        0 -> {
                            editingBank = null
                            showBankSheet = true
                        }
                        1 -> {
                            editingStock = null
                            showStockSheet = true
                        }
                        2 -> {
                            editingCard = null
                            showCardSheet = true
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Shared tab row
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.background
            ) {
                assetTabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        text = { Text(tab.label) }
                    )
                }
            }

            // Pager hosting content screens
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    0 -> BanksScreen(
                        viewModel = banksViewModel,
                        onEditBank = { bank ->
                            editingBank = bank
                            showBankSheet = true
                        }
                    )
                    1 -> StocksScreen(
                        viewModel = stocksViewModel,
                        onEditStock = { stock ->
                            editingStock = stock
                            showStockSheet = true
                        }
                    )
                    2 -> CreditCardsScreen(
                        viewModel = creditCardsViewModel,
                        onEditCard = { card ->
                            editingCard = card
                            showCardSheet = true
                        }
                    )
                }
            }
        }
    }

    if (showBankSheet) {
        val banksUiState by banksViewModel.uiState.collectAsState()
        com.nooblabs.folio.ui.banks.BankFormBottomSheet(
            bankToEdit = editingBank,
            globalCurrency = banksUiState.globalCurrency,
            onDismiss = { showBankSheet = false },
            onSave = { name, number, balance, currency ->
                if (editingBank != null) {
                    banksViewModel.updateBank(editingBank!!.id, name, number, balance, currency)
                } else {
                    banksViewModel.addBank(name, number, balance, currency)
                }
                showBankSheet = false
            }
        )
    }

    if (showStockSheet) {
        val stocksUiState by stocksViewModel.uiState.collectAsState()
        com.nooblabs.folio.ui.stocks.StockFormBottomSheet(
            stockToEdit = editingStock,
            onDismiss = { showStockSheet = false },
            onSave = { ticker, quantity, avgPrice, purchaseDate ->
                if (editingStock != null) {
                    stocksViewModel.updateStock(editingStock!!.id, ticker, quantity, avgPrice, purchaseDate)
                } else {
                    stocksViewModel.addStock(ticker, quantity, avgPrice, purchaseDate)
                }
                showStockSheet = false
            },
            cachedPrices = stocksUiState.prices,
            globalCurrency = stocksUiState.globalCurrency,
            onFetchPrice = { ticker -> stocksViewModel.fetchSinglePrice(ticker) },
            isApiKeySet = stocksUiState.isApiKeySet
        )
    }

    if (showCardSheet) {
        val cardsUiState by creditCardsViewModel.uiState.collectAsState()
        com.nooblabs.folio.ui.creditcards.CardFormBottomSheet(
            cardToEdit = editingCard,
            globalCurrency = cardsUiState.globalCurrency,
            onDismiss = { showCardSheet = false },
            onSave = { name, number, limit, outstanding, dueDate, expiry ->
                if (editingCard != null) {
                    creditCardsViewModel.updateCreditCard(
                        editingCard!!.id,
                        name,
                        number,
                        limit,
                        outstanding,
                        dueDate,
                        expiry
                    )
                } else {
                    creditCardsViewModel.addCreditCard(name, number, limit, outstanding, dueDate, expiry)
                }
                showCardSheet = false
            },
            onDelete = { card ->
                creditCardsViewModel.deleteCreditCard(card)
                showCardSheet = false
            }
        )
    }
}
