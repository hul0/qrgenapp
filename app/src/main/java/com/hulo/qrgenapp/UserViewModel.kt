package com.hulo.qrgenapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserUiState(
    val coins: Int = 0,
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
                val currentCoins = userPreferences.getCoins()
                if (userPreferences.isFirstLaunch()) {
                    // Initialize with 30 coins on first launch
                    userPreferences.setCoins(30)
                    userPreferences.setFirstLaunch(false)
                    _uiState.update { it.copy(coins = 30, isLoading = false) }
                } else {
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
