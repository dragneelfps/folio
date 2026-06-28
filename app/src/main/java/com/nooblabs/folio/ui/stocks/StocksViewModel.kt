package com.nooblabs.folio.ui.stocks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nooblabs.folio.domain.model.StockInvestment
import com.nooblabs.folio.domain.repository.SettingsRepository
import com.nooblabs.folio.domain.repository.StockInvestmentRepository
import com.nooblabs.folio.domain.util.CurrencyConverter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.nooblabs.folio.data.repository.LiveStockPriceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

data class StockUiModel(
    val stock: StockInvestment,
    val convertedCurrentPrice: Double,
    val convertedAverageBuyPrice: Double,
    val convertedTotalValue: Double,
    val convertedTotalCost: Double,
    val globalCurrency: String
)

data class StocksUiState(
    val groupedStocks: Map<String, List<StockUiModel>> = emptyMap(),
    val globalCurrency: String = "USD",
    val isSyncing: Boolean = false,
    val lastUpdated: Long? = null,
    val isApiKeySet: Boolean = false,
    val prices: Map<String, Double> = emptyMap(),
    val error: String? = null
)

class StocksViewModel(
    private val repository: StockInvestmentRepository,
    private val liveStockPriceRepository: LiveStockPriceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<StocksUiState> = combine(
        combine(
            repository.getAllStockInvestments(),
            liveStockPriceRepository.prices,
            settingsRepository.globalCurrency
        ) { stocks, prices, currency -> Triple(stocks, prices, currency) },
        combine(
            liveStockPriceRepository.isSyncing,
            liveStockPriceRepository.globalLastUpdated,
            settingsRepository.finnhubApiKey
        ) { syncing, updated, key -> Triple(syncing, updated, key) },
        _error
    ) { first, second, error ->
        val stocks = first.first
        val prices = first.second
        val globalCurrency = first.third

        val isSyncing = second.first
        val lastUpdated = second.second
        val apiKey = second.third

        val uiModels = stocks.map { stock ->
            val currentPrice = prices[stock.tickerSymbol] ?: 0.0
            val convCurrent = CurrencyConverter.convert(currentPrice, "USD", globalCurrency)
            val convAvg = CurrencyConverter.convert(stock.averageBuyPrice, "USD", globalCurrency)
            
            StockUiModel(
                stock = stock,
                convertedCurrentPrice = convCurrent,
                convertedAverageBuyPrice = convAvg,
                convertedTotalValue = convCurrent * stock.quantity,
                convertedTotalCost = convAvg * stock.quantity,
                globalCurrency = globalCurrency
            )
        }
        val grouped = uiModels.groupBy { it.stock.tickerSymbol.uppercase() }
        StocksUiState(
            groupedStocks = grouped,
            globalCurrency = globalCurrency,
            isSyncing = isSyncing,
            lastUpdated = lastUpdated,
            isApiKeySet = apiKey.isNotBlank(),
            prices = prices,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StocksUiState()
    )

    init {
        viewModelScope.launch {
            repository.getAllStockInvestments().collect { stocks ->
                if (stocks.isNotEmpty()) {
                    val tickers = stocks.map { it.tickerSymbol }.distinct()
                    try {
                        liveStockPriceRepository.syncPrices(tickers)
                    } catch (e: Exception) {
                        _error.value = e.localizedMessage ?: "Failed to sync stock prices"
                    }
                }
            }
        }
    }

    fun syncPrices() {
        val currentStocks = uiState.value.groupedStocks.keys.toList()
        try {
            liveStockPriceRepository.syncPrices(currentStocks, force = true)
        } catch (e: Exception) {
            _error.value = e.localizedMessage ?: "Failed to sync stock prices"
        }
    }

    fun addStock(ticker: String, quantity: Double, averageBuyPrice: Double, purchaseDate: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            try {
                repository.insertStockInvestment(
                    StockInvestment(
                        tickerSymbol = ticker.uppercase(),
                        quantity = quantity,
                        averageBuyPrice = averageBuyPrice,
                        purchaseDate = purchaseDate
                    )
                )
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Failed to add stock investment"
            }
        }
    }

    fun updateStock(id: Int, ticker: String, quantity: Double, averageBuyPrice: Double, purchaseDate: Long) {
        viewModelScope.launch {
            try {
                repository.updateStockInvestment(
                    StockInvestment(
                        id = id,
                        tickerSymbol = ticker.uppercase(),
                        quantity = quantity,
                        averageBuyPrice = averageBuyPrice,
                        purchaseDate = purchaseDate
                    )
                )
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Failed to update stock investment"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun setGlobalCurrency(currency: String) {
        settingsRepository.setGlobalCurrency(currency)
    }

    suspend fun fetchSinglePrice(ticker: String): Double? {
        if (ticker.isBlank()) return null
        return try {
            liveStockPriceRepository.fetchSinglePriceDirectly(ticker.uppercase().trim())
        } catch (e: Exception) {
            _error.value = e.localizedMessage ?: "Failed to fetch stock price"
            null
        }
    }
}
