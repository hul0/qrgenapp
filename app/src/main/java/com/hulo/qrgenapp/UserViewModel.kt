// file: UserViewModel.kt
package com.hulo.qrgenapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserUiState(
    // Initialize with the default starting coins for UI display, will be updated by loadUserCoins()
    val coins: Int = UserPreferences.DEFAULT_STARTING_COINS,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class UserViewModel(private val userPreferences: UserPreferences) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    init {
        loadUserCoins()
    }

    private fun loadUserCoins() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                if (userPreferences.isFirstLaunch()) {
                    // Initialize with 10 coins on the very first launch
                    userPreferences.setCoins(UserPreferences.DEFAULT_STARTING_COINS)
                    userPreferences.setFirstLaunch(false) // Mark as not the first launch
                    _uiState.update { it.copy(coins = UserPreferences.DEFAULT_STARTING_COINS, isLoading = false) }
                } else {
                    // For subsequent launches, load the existing balance
                    val currentCoins = userPreferences.getCoins()
                    _uiState.update { it.copy(coins = currentCoins, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to load coins: ${e.message}", isLoading = false) }
            }
        }
    }

    fun addCoins(amount: Int) {
        viewModelScope.launch {
            val currentCoins = userPreferences.getCoins()
            val newCoins = currentCoins + amount
            userPreferences.setCoins(newCoins)
            _uiState.update { it.copy(coins = newCoins) }
        }
    }

    fun deductCoins(amount: Int): Boolean {
        return if (_uiState.value.coins >= amount) {
            viewModelScope.launch {
                val currentCoins = userPreferences.getCoins()
                val newCoins = currentCoins - amount
                userPreferences.setCoins(newCoins)
                _uiState.update { it.copy(coins = newCoins) }
            }
            true
        } else {
            false // Not enough coins
        }
    }
}