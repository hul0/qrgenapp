// file: UserViewModel.kt
package com.hulo.qrgenapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest

data class UserUiState(
    val coins: Int = UserPreferences.DEFAULT_STARTING_COINS,
    val diamonds: Int = UserPreferences.DEFAULT_STARTING_DIAMONDS, // New: Diamonds
    val isPremium: Boolean = false, // New: Premium status
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val redeemedCodes: Set<String> = emptySet(), // New: Track redeemed codes
    val scanHistory: List<String> = emptyList() // New: Scan history
)

class UserViewModel(private val userPreferences: UserPreferences) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    // Constants for redeem code and premium
    companion object {
        private const val PREMIUM_COST_DIAMONDS = 1000
        private const val REDEEM_CODE_NEW_LAUNCH = "NEWLAUNCH1000"
        private const val REDEEM_REWARD_DIAMONDS = 1000
        private const val REDEEM_SALT = "qrwiz_salt_2025_v1" // Salt for hashing
    }

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                if (userPreferences.isFirstLaunch()) {
                    // Initialize with default coins and diamonds on first launch
                    userPreferences.setCoins(UserPreferences.DEFAULT_STARTING_COINS)
                    userPreferences.setDiamonds(UserPreferences.DEFAULT_STARTING_DIAMONDS)
                    userPreferences.setPremium(false) // Ensure premium is false on first launch
                    userPreferences.setFirstLaunch(false) // Mark as not the first launch
                    _uiState.update {
                        it.copy(
                            coins = UserPreferences.DEFAULT_STARTING_COINS,
                            diamonds = UserPreferences.DEFAULT_STARTING_DIAMONDS,
                            isPremium = false,
                            isLoading = false
                        )
                    }
                } else {
                    // For subsequent launches, load the existing balances and status
                    val currentCoins = userPreferences.getCoins()
                    val currentDiamonds = userPreferences.getDiamonds()
                    val currentPremiumStatus = userPreferences.isPremium()
                    val currentRedeemedCodes = userPreferences.getRedeemedCodes()
                    val currentScanHistory = userPreferences.getScanHistory()

                    _uiState.update {
                        it.copy(
                            coins = currentCoins,
                            diamonds = currentDiamonds,
                            isPremium = currentPremiumStatus,
                            redeemedCodes = currentRedeemedCodes,
                            scanHistory = currentScanHistory,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to load user data: ${e.message}", isLoading = false) }
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

    fun addDiamonds(amount: Int) {
        viewModelScope.launch {
            val currentDiamonds = userPreferences.getDiamonds()
            val newDiamonds = currentDiamonds + amount
            userPreferences.setDiamonds(newDiamonds)
            _uiState.update { it.copy(diamonds = newDiamonds) }
        }
    }

    fun deductDiamonds(amount: Int): Boolean {
        return if (_uiState.value.diamonds >= amount) {
            viewModelScope.launch {
                val currentDiamonds = userPreferences.getDiamonds()
                val newDiamonds = currentDiamonds - amount
                userPreferences.setDiamonds(newDiamonds)
                _uiState.update { it.copy(diamonds = newDiamonds) }
            }
            true
        } else {
            false // Not enough diamonds
        }
    }

    fun buyPremium(): Boolean {
        return if (_uiState.value.isPremium) {
            // Already premium
            false
        } else if (_uiState.value.diamonds >= PREMIUM_COST_DIAMONDS) {
            viewModelScope.launch {
                val currentDiamonds = userPreferences.getDiamonds()
                val newDiamonds = currentDiamonds - PREMIUM_COST_DIAMONDS
                userPreferences.setDiamonds(newDiamonds)
                userPreferences.setPremium(true)
                _uiState.update { it.copy(diamonds = newDiamonds, isPremium = true) }
            }
            true
        } else {
            false // Not enough diamonds
        }
    }

    fun redeemCode(code: String): String {
        val hashedCode = hashWithSHA512(code + REDEEM_SALT) // Apply salt before hashing

        // Hardcoded redeem codes and their hashes
        val newLaunchCodeHash = hashWithSHA512(REDEEM_CODE_NEW_LAUNCH + REDEEM_SALT)

        return when (hashedCode) {
            newLaunchCodeHash -> {
                if (userPreferences.getRedeemedCodes().contains(hashedCode)) {
                    "Code already redeemed!"
                } else {
                    addDiamonds(REDEEM_REWARD_DIAMONDS)
                    userPreferences.addRedeemedCode(hashedCode)
                    "Successfully redeemed $REDEEM_REWARD_DIAMONDS Diamonds!"
                }
            }
            else -> "Invalid redeem code."
        }
    }

    private fun hashWithSHA512(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-512")
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }

    fun addScanToHistory(scanResult: String) {
        viewModelScope.launch {
            userPreferences.addScanToHistory(scanResult, _uiState.value.isPremium)
            _uiState.update { it.copy(scanHistory = userPreferences.getScanHistory()) }
        }
    }

    fun clearScanHistory() {
        viewModelScope.launch {
            userPreferences.clearScanHistory()
            _uiState.update { it.copy(scanHistory = emptyList()) }
        }
    }
}
