package com.nooblabs.folio.domain.model

data class CreditCard(
    val id: Int = 0,
    val cardName: String,
    val cardNumber: String,
    val creditLimit: Double,
    val currentOutstanding: Double,
    val dueDate: Long,
    val expiry: String
)
