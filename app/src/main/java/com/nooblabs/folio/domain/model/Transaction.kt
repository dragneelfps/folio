package com.nooblabs.folio.domain.model

data class Transaction(
    val id: Int = 0,
    val amount: Double,
    val date: Long,
    val description: String,
    val type: String, // "CREDIT" or "DEBIT"
    val category: String,
    val sourceId: Int,
    val sourceType: String, // "BANK" or "CARD"
    val currency: String = "USD"
)
