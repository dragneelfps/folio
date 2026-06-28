package com.nooblabs.folio.data.repository

import android.content.ContentValues
import com.nooblabs.folio.data.local.DatabaseHelper
import com.nooblabs.folio.data.network.FinnhubService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LiveStockPriceRepository(
    private val dbHelper: DatabaseHelper,
    private val finnhubService: FinnhubService,
    private val scope: CoroutineScope
) {
    private val _prices = MutableStateFlow<Map<String, Double>>(emptyMap())
    val prices: StateFlow<Map<String, Double>> = _prices.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _globalLastUpdated = MutableStateFlow<Long?>(null)
    val globalLastUpdated: StateFlow<Long?> = _globalLastUpdated.asStateFlow()

    private val lastUpdatedMap = mutableMapOf<String, Long>()
    private val cacheTtlMillis = 3600 * 1000L // 1 hour

    init {
        loadCacheFromDb()
    }

    private fun loadCacheFromDb() {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM stock_prices_cache", null)
        val initialPrices = mutableMapOf<String, Double>()
        var maxUpdated: Long? = null
        cursor.use {
            while (it.moveToNext()) {
                val ticker = it.getString(it.getColumnIndexOrThrow("tickerSymbol"))
                val price = it.getDouble(it.getColumnIndexOrThrow("price"))
                val lastUpdated = it.getLong(it.getColumnIndexOrThrow("lastUpdated"))
                initialPrices[ticker] = price
                lastUpdatedMap[ticker] = lastUpdated
                
                if (maxUpdated == null || lastUpdated > maxUpdated!!) {
                    maxUpdated = lastUpdated
                }
            }
        }
        _prices.value = initialPrices
        _globalLastUpdated.value = maxUpdated
    }

    private fun saveToDb(ticker: String, price: Double, lastUpdated: Long) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("tickerSymbol", ticker)
            put("price", price)
            put("lastUpdated", lastUpdated)
        }
        db.insertWithOnConflict("stock_prices_cache", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun syncPrices(tickers: List<String>, force: Boolean = false) {
        if (tickers.isEmpty()) return
        scope.launch {
            _isSyncing.value = true
            val updatedPrices = _prices.value.toMutableMap()
            var changed = false
            val currentTime = System.currentTimeMillis()
            var latestUpdate = _globalLastUpdated.value

            for (ticker in tickers) {
                val lastUpdated = lastUpdatedMap[ticker] ?: 0L
                val isStale = (currentTime - lastUpdated) > cacheTtlMillis

                if (isStale || force) {
                    val newPrice = finnhubService.fetchCurrentPrice(ticker)
                    if (newPrice != null && newPrice > 0.0) {
                        updatedPrices[ticker] = newPrice
                        lastUpdatedMap[ticker] = currentTime
                        saveToDb(ticker, newPrice, currentTime)
                        changed = true
                        latestUpdate = currentTime
                    }
                }
            }
            if (changed) {
                _prices.value = updatedPrices
                _globalLastUpdated.value = latestUpdate
            }
            _isSyncing.value = false
        }
    }

    suspend fun fetchSinglePriceDirectly(ticker: String): Double? {
        val newPrice = finnhubService.fetchCurrentPrice(ticker)
        if (newPrice != null && newPrice > 0.0) {
            val currentTime = System.currentTimeMillis()
            saveToDb(ticker, newPrice, currentTime)
            val updatedPrices = _prices.value.toMutableMap()
            updatedPrices[ticker] = newPrice
            _prices.value = updatedPrices
            lastUpdatedMap[ticker] = currentTime
            _globalLastUpdated.value = currentTime
            return newPrice
        }
        return null
    }
}
