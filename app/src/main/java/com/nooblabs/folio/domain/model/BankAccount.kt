package com.nooblabs.folio.domain.model

data class BankAccount(
    val id: Int = 0,
    val bankName: String,
    val accountNumber: String,
    val balance: Double,
    val currency: String
)
