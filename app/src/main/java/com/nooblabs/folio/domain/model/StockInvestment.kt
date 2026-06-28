package com.nooblabs.folio.domain.model

data class StockInvestment(
    val id: Int = 0,
    val tickerSymbol: String,
    val quantity: Double,
    val averageBuyPrice: Double,
    val purchaseDate: Long = System.currentTimeMillis()
)
