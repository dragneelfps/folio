package com.nooblabs.folio.domain.util

object CurrencyConverter {
    val supportedCurrencies = listOf("USD", "EUR", "GBP", "INR", "JPY", "CAD", "AUD")

    // Hardcoded rates relative to USD (1 USD = X Currency) for Offline MVP
    private val exchangeRatesToUsd = mapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "GBP" to 0.79,
        "INR" to 83.3,
        "JPY" to 151.0,
        "CAD" to 1.36,
        "AUD" to 1.52
    )

    fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency == toCurrency) return amount
        
        val fromRate = exchangeRatesToUsd[fromCurrency.uppercase()] ?: 1.0
        val toRate = exchangeRatesToUsd[toCurrency.uppercase()] ?: 1.0
        
        // Convert to USD first, then to target currency
        val amountInUsd = amount / fromRate
        return amountInUsd * toRate
    }
}
