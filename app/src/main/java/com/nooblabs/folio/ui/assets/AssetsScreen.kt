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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
                modifier = Modifier.fillMaxWidth()
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
                    0 -> BanksScreen(viewModel = banksViewModel)
                    1 -> StocksScreen(viewModel = stocksViewModel)
                    2 -> CreditCardsScreen(viewModel = creditCardsViewModel)
                }
            }
        }
    }
}
