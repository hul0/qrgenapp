package com.hulo.qrgenapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class UserUiState(
    val coins: Int = UserPreferences.DEFAULT_STARTING_COINS,
    val diamonds: Int = UserPreferences.DEFAULT_STARTING_DIAMONDS, // New: Diamonds
    val isPremium: Boolean = false, // New: Premium status
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val redeemedCodes: Set<String> = emptySet(), // New: Track redeemed codes
    val scanHistory: List<String> = emptyList(), // New: Scan history
    val lastLoginDate: String? = null, // New: Last login date for daily bonus
    val dailyStreak: Int = 0, // New: Daily login streak (0-6 for weekly cycle)
    val dailyBonusAvailable: Boolean = false, // New: Flag for daily bonus availability
    val dailyBonusAmount: Int = 0, // New: Amount of daily bonus coins
    val dailyBonusPattern: List<Int> = emptyList(), // New: Expose the daily bonus pattern
    val firstUpdateWarningTime: Long = 0L // New: Timestamp for the first update warning
)

class UserViewModel(private val userPreferences: UserPreferences) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    // Constants for redeem code and premium
    companion object {
        private const val PREMIUM_COST_DIAMONDS = 1000
        private const val REDEEM_CODE_NEW_LAUNCH = "Freedom"
        private const val REDEEM_REWARD_DIAMONDS = 10000
        private const val REDEEM_SALT = "qrwiz_salt_2025_v1" // Salt for hashing

        // Daily bonus coin pattern (7 days for a weekly reset)
        private val DAILY_BONUS_COINS = listOf(15, 25, 35, 50, 60, 80, 100)
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
                    // Initialize daily login preferences
                    userPreferences.setLastLoginDate(null)
                    userPreferences.setDailyStreak(0) // Start streak at 0
                    // Initialize update warning time
                    userPreferences.clearFirstUpdateWarningTime() // Ensure it's clear on first launch

                    _uiState.update {
                        it.copy(
                            coins = UserPreferences.DEFAULT_STARTING_COINS,
                            diamonds = UserPreferences.DEFAULT_STARTING_DIAMONDS,
                            isPremium = false,
                            lastLoginDate = null,
                            dailyStreak = 0,
                            isLoading = false,
                            dailyBonusPattern = DAILY_BONUS_COINS, // Initialize pattern
                            firstUpdateWarningTime = userPreferences.getFirstUpdateWarningTime() // Load update warning time
                        )
                    }
                } else {
                    // For subsequent launches, load the existing balances and status
                    val currentCoins = userPreferences.getCoins()
                    val currentDiamonds = userPreferences.getDiamonds()
                    val currentPremiumStatus = userPreferences.isPremium()
                    val currentRedeemedCodes = userPreferences.getRedeemedCodes()
                    val currentScanHistory = userPreferences.getScanHistory()
                    val lastLoginDate = userPreferences.getLastLoginDate()
                    val dailyStreak = userPreferences.getDailyStreak()
                    val firstUpdateWarningTime = userPreferences.getFirstUpdateWarningTime() // Load update warning time

                    _uiState.update {
                        it.copy(
                            coins = currentCoins,
                            diamonds = currentDiamonds,
                            isPremium = currentPremiumStatus,
                            redeemedCodes = currentRedeemedCodes,
                            scanHistory = currentScanHistory,
                            lastLoginDate = lastLoginDate,
                            dailyStreak = dailyStreak,
                            isLoading = false,
                            dailyBonusPattern = DAILY_BONUS_COINS, // Initialize pattern
                            firstUpdateWarningTime = firstUpdateWarningTime // Update UI state with loaded time
                        )
                    }
                }
                checkDailyLoginBonus() // Check daily bonus after loading user data
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
            userPreferences.addScanToHistory(scanResult, _uiState.value.isPremium) // Use userPreferences.isPremium()
            _uiState.update { it.copy(scanHistory = userPreferences.getScanHistory()) }
        }
    }

    fun clearScanHistory() {
        viewModelScope.launch {
            userPreferences.clearScanHistory()
            _uiState.update { it.copy(scanHistory = emptyList()) }
        }
    }

    fun checkDailyLoginBonus() {
        viewModelScope.launch {
            val lastLoginDateStr = userPreferences.getLastLoginDate()
            val currentStreak = userPreferences.getDailyStreak() // This is the 0-6 index
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            if (lastLoginDateStr == null || lastLoginDateStr != today) {
                // Not logged in today, or first login
                val calendar = Calendar.getInstance()
                val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                    calendar.apply { add(Calendar.DATE, -1) }.time
                )

                if (lastLoginDateStr == yesterday) {
                    // Logged in yesterday, continue streak
                    // Increment streak, and apply modulo for weekly reset
                    val nextStreakIndex = (currentStreak + 1) % DAILY_BONUS_COINS.size
                    val bonusAmount = DAILY_BONUS_COINS[nextStreakIndex]
                    _uiState.update {
                        it.copy(
                            dailyBonusAvailable = true,
                            dailyBonusAmount = bonusAmount,
                            dailyStreak = nextStreakIndex // Update streak for UI, will be saved on claim
                        )
                    }
                    // Log the streak as 1-based for user understanding
                    Log.d("DailyBonus", "Daily bonus available: $bonusAmount coins for streak ${nextStreakIndex + 1}")
                } else {
                    // Missed a day or first login, reset streak to 0
                    val bonusAmount = DAILY_BONUS_COINS[0]
                    _uiState.update {
                        it.copy(
                            dailyBonusAvailable = true,
                            dailyBonusAmount = bonusAmount,
                            dailyStreak = 0 // Reset streak to 0
                        )
                    }
                    Log.d("DailyBonus", "Daily bonus available: $bonusAmount coins (streak reset)")
                }
            } else {
                // Already logged in today
                _uiState.update {
                    it.copy(
                        dailyBonusAvailable = false,
                        dailyBonusAmount = 0
                    )
                }
                Log.d("DailyBonus", "Already claimed daily bonus today.")
            }
        }
    }

    fun claimDailyBonus() {
        viewModelScope.launch {
            _uiState.update { currentState ->
                val newCoins = currentState.coins + currentState.dailyBonusAmount
                // The streak for the *next* day is calculated here and saved.
                // It's the current streak + 1, modulo the size of the bonus list.
                val nextDayStreak = (currentState.dailyStreak + 1) % DAILY_BONUS_COINS.size

                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                userPreferences.setCoins(newCoins)
                userPreferences.setLastLoginDate(today)
                userPreferences.setDailyStreak(nextDayStreak) // Save the streak for the next day

                Log.d("DailyBonus", "Claimed ${currentState.dailyBonusAmount} coins. New balance: $newCoins, Next day's streak will be: ${nextDayStreak + 1}")

                currentState.copy(
                    coins = newCoins,
                    lastLoginDate = today,
                    dailyStreak = nextDayStreak, // Update UI state with the saved streak
                    dailyBonusAvailable = false,
                    dailyBonusAmount = 0
                )
            }
        }
    }
}