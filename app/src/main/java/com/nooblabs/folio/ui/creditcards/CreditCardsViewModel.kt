package com.nooblabs.folio.ui.creditcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nooblabs.folio.domain.model.CreditCard
import com.nooblabs.folio.domain.repository.CreditCardRepository
import com.nooblabs.folio.domain.repository.SettingsRepository
import com.nooblabs.folio.domain.util.CurrencyConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CreditCardUiModel(
    val card: CreditCard,
    val convertedOutstanding: Double,
    val convertedLimit: Double,
    val globalCurrency: String
)

data class CreditCardsUiState(
    val cards: List<CreditCardUiModel> = emptyList(),
    val globalCurrency: String = "USD",
    val error: String? = null
)

class CreditCardsViewModel(
    private val repository: CreditCardRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CreditCardsUiState> = combine(
        repository.getAllCreditCards(),
        settingsRepository.globalCurrency,
        _error
    ) { cards, globalCurrency, error ->
        val mapped = cards.map { card ->
            CreditCardUiModel(
                card = card,
                convertedOutstanding = CurrencyConverter.convert(card.currentOutstanding, "USD", globalCurrency),
                convertedLimit = CurrencyConverter.convert(card.creditLimit, "USD", globalCurrency),
                globalCurrency = globalCurrency
            )
        }
        CreditCardsUiState(cards = mapped, globalCurrency = globalCurrency, error = error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CreditCardsUiState()
    )

    fun addCreditCard(
        cardName: String,
        cardNumber: String,
        creditLimit: Double,
        currentOutstanding: Double,
        dueDate: Long,
        expiry: String
    ) {
        viewModelScope.launch {
            try {
                repository.insertCreditCard(
                    CreditCard(
                        cardName = cardName,
                        cardNumber = cardNumber,
                        creditLimit = creditLimit,
                        currentOutstanding = currentOutstanding,
                        dueDate = dueDate,
                        expiry = expiry
                    )
                )
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Failed to add credit card"
            }
        }
    }

    fun updateCreditCard(
        id: Int,
        cardName: String,
        cardNumber: String,
        creditLimit: Double,
        currentOutstanding: Double,
        dueDate: Long,
        expiry: String
    ) {
        viewModelScope.launch {
            try {
                repository.updateCreditCard(
                    CreditCard(
                        id = id,
                        cardName = cardName,
                        cardNumber = cardNumber,
                        creditLimit = creditLimit,
                        currentOutstanding = currentOutstanding,
                        dueDate = dueDate,
                        expiry = expiry
                    )
                )
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Failed to update credit card"
            }
        }
    }

    fun deleteCreditCard(card: CreditCard) {
        viewModelScope.launch {
            try {
                repository.deleteCreditCard(card)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Failed to delete credit card"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun setGlobalCurrency(currency: String) {
        settingsRepository.setGlobalCurrency(currency)
    }
}
