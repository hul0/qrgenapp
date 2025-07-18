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

// --- MODIFIED: Added LTV tracking fields ---
data class UserUiState(
    val coins: Int = UserPreferences.DEFAULT_STARTING_COINS,
    val diamonds: Int = UserPreferences.DEFAULT_STARTING_DIAMONDS,
    val isPremium: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val redeemedCodes: Set<String> = emptySet(),
    val scanHistory: List<String> = emptyList(),
    val lastLoginDate: String? = null,
    val dailyStreak: Int = 0,
    val dailyBonusAvailable: Boolean = false,
    val dailyBonusAmount: Int = 0,
    val dailyBonusPattern: List<Int> = emptyList(),
    val firstUpdateWarningTime: Long = 0L,

    // --- NEW: LTV & Ad Analytics State ---
    val bannerImpressions: Int = 0,
    val interstitialImpressions: Int = 0,
    val rewardedImpressions: Int = 0,
    val rewardedInterstitialImpressions: Int = 0,
    val nativeImpressions: Int = 0,
    val totalLtvInr: Double = 0.0
)

class UserViewModel(private val userPreferences: UserPreferences) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    companion object {
        private const val PREMIUM_COST_DIAMONDS = 1000
        private const val REDEEM_CODE_NEW_LAUNCH = "Freedom"
        private const val REDEEM_REWARD_DIAMONDS = 10000
        private const val REDEEM_SALT = "qrwiz_salt_2025_v1"
        private val DAILY_BONUS_COINS = listOf(15, 25, 35, 50, 60, 80, 100)

        // --- NEW: Hardcoded eCPM values for testing (in INR) ---
        // You can change these values to whatever you need for testing.
        private const val ECPM_BANNER_INR = 8.0                  //  ₹40 per 1000 impressions
        private const val ECPM_INTERSTITIAL_INR = 40.0           // ₹250 per 1000 impressions
        private const val ECPM_REWARDED_INR = 90.0                 // ₹450 per 1000 impressions
        private const val ECPM_REWARDED_INTERSTITIAL_INR = 40.0  // ₹500 per 1000 impressions
        private const val ECPM_NATIVE_INR = 8.0                 // ₹200 per 1000 impressions
    }

    init {
        loadUserData()
    }

    // --- MODIFIED: To load LTV data alongside your existing data ---
    private fun loadUserData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Load LTV data from preferences first
                val bannerImpressions = userPreferences.getBannerImpressions()
                val interstitialImpressions = userPreferences.getInterstitialImpressions()
                val rewardedImpressions = userPreferences.getRewardedImpressions()
                val rewardedInterstitialImpressions = userPreferences.getRewardedInterstitialImpressions()
                val nativeImpressions = userPreferences.getNativeImpressions()
                val totalLtvMicros = userPreferences.getTotalLtvMicros()
                val totalLtvInr = totalLtvMicros / 1_000_000.0

                if (userPreferences.isFirstLaunch()) {
                    // Your original first-launch logic
                    userPreferences.setCoins(UserPreferences.DEFAULT_STARTING_COINS)
                    userPreferences.setDiamonds(UserPreferences.DEFAULT_STARTING_DIAMONDS)
                    userPreferences.setPremium(false)
                    userPreferences.setFirstLaunch(false)
                    userPreferences.setLastLoginDate(null)
                    userPreferences.setDailyStreak(0)
                    userPreferences.clearFirstUpdateWarningTime()

                    // Update UI state including LTV data
                    _uiState.update {
                        it.copy(
                            coins = UserPreferences.DEFAULT_STARTING_COINS,
                            diamonds = UserPreferences.DEFAULT_STARTING_DIAMONDS,
                            isPremium = false,
                            lastLoginDate = null,
                            dailyStreak = 0,
                            isLoading = false,
                            dailyBonusPattern = DAILY_BONUS_COINS,
                            firstUpdateWarningTime = userPreferences.getFirstUpdateWarningTime(),
                            bannerImpressions = bannerImpressions,
                            interstitialImpressions = interstitialImpressions,
                            rewardedImpressions = rewardedImpressions,
                            rewardedInterstitialImpressions = rewardedInterstitialImpressions,
                            nativeImpressions = nativeImpressions,
                            totalLtvInr = totalLtvInr
                        )
                    }
                } else {
                    // Your original logic for returning users
                    val currentCoins = userPreferences.getCoins()
                    val currentDiamonds = userPreferences.getDiamonds()
                    val currentPremiumStatus = userPreferences.isPremium()
                    val currentRedeemedCodes = userPreferences.getRedeemedCodes()
                    val currentScanHistory = userPreferences.getScanHistory()
                    val lastLoginDate = userPreferences.getLastLoginDate()
                    val dailyStreak = userPreferences.getDailyStreak()
                    val firstUpdateWarningTime = userPreferences.getFirstUpdateWarningTime()

                    // Update UI state including LTV data
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
                            dailyBonusPattern = DAILY_BONUS_COINS,
                            firstUpdateWarningTime = firstUpdateWarningTime,
                            bannerImpressions = bannerImpressions,
                            interstitialImpressions = interstitialImpressions,
                            rewardedImpressions = rewardedImpressions,
                            rewardedInterstitialImpressions = rewardedInterstitialImpressions,
                            nativeImpressions = nativeImpressions,
                            totalLtvInr = totalLtvInr
                        )
                    }
                }
                checkDailyLoginBonus()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to load user data: ${e.message}", isLoading = false) }
            }
        }
    }

    // --- YOUR ORIGINAL WORKING FUNCTIONS (UNCHANGED) ---

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
            false
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
            false
        }
    }

    fun buyPremium(): Boolean {
        return if (_uiState.value.isPremium) {
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
            false
        }
    }

    fun redeemCode(code: String): String {
        val hashedCode = hashWithSHA512(code + REDEEM_SALT)
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
        return digest.fold("") { str, it -> str + "%02x".format(it) }
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

    fun checkDailyLoginBonus() {
        viewModelScope.launch {
            val lastLoginDateStr = userPreferences.getLastLoginDate()
            val currentStreak = userPreferences.getDailyStreak()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            if (lastLoginDateStr == null || lastLoginDateStr != today) {
                val calendar = Calendar.getInstance()
                val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                    calendar.apply { add(Calendar.DATE, -1) }.time
                )

                if (lastLoginDateStr == yesterday) {
                    val nextStreakIndex = (currentStreak + 1) % DAILY_BONUS_COINS.size
                    val bonusAmount = DAILY_BONUS_COINS[nextStreakIndex]
                    _uiState.update {
                        it.copy(
                            dailyBonusAvailable = true,
                            dailyBonusAmount = bonusAmount,
                            dailyStreak = nextStreakIndex
                        )
                    }
                    Log.d("DailyBonus", "Daily bonus available: $bonusAmount coins for streak ${nextStreakIndex + 1}")
                } else {
                    val bonusAmount = DAILY_BONUS_COINS[0]
                    _uiState.update {
                        it.copy(
                            dailyBonusAvailable = true,
                            dailyBonusAmount = bonusAmount,
                            dailyStreak = 0
                        )
                    }
                    Log.d("DailyBonus", "Daily bonus available: $bonusAmount coins (streak reset)")
                }
            } else {
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
                val nextDayStreak = (currentState.dailyStreak + 1) % DAILY_BONUS_COINS.size
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                userPreferences.setCoins(newCoins)
                userPreferences.setLastLoginDate(today)
                userPreferences.setDailyStreak(nextDayStreak)

                Log.d("DailyBonus", "Claimed ${currentState.dailyBonusAmount} coins. New balance: $newCoins, Next day's streak will be: ${nextDayStreak + 1}")

                currentState.copy(
                    coins = newCoins,
                    lastLoginDate = today,
                    dailyStreak = nextDayStreak,
                    dailyBonusAvailable = false,
                    dailyBonusAmount = 0
                )
            }
        }
    }

    // --- NEW LTV & AD ANALYTICS FUNCTIONS (Using Hardcoded eCPM) ---

    private fun addSimulatedRevenue(ecpm: Double) {
        // Calculate revenue for a single impression from the eCPM value
        val revenueForOneImpression = ecpm / 2000.0
        // Convert to micros (Long) to store without floating point errors
        val revenueMicros = (revenueForOneImpression * 1_000_000).toLong()
        userPreferences.addRevenueMicros(revenueMicros)
    }

    fun trackBannerImpression() {
        viewModelScope.launch {
            addSimulatedRevenue(ECPM_BANNER_INR)
            userPreferences.incrementBannerImpressions()
            _uiState.update {
                it.copy(
                    bannerImpressions = userPreferences.getBannerImpressions(),
                    totalLtvInr = userPreferences.getTotalLtvMicros() / 1_000_000.0
                )
            }
        }
    }

    fun trackInterstitialImpression() {
        viewModelScope.launch {
            addSimulatedRevenue(ECPM_INTERSTITIAL_INR)
            userPreferences.incrementInterstitialImpressions()
            _uiState.update {
                it.copy(
                    interstitialImpressions = userPreferences.getInterstitialImpressions(),
                    totalLtvInr = userPreferences.getTotalLtvMicros() / 1_000_000.0
                )
            }
        }
    }

    fun trackRewardedImpression() {
        viewModelScope.launch {
            addSimulatedRevenue(ECPM_REWARDED_INR)
            userPreferences.incrementRewardedImpressions()
            _uiState.update {
                it.copy(
                    rewardedImpressions = userPreferences.getRewardedImpressions(),
                    totalLtvInr = userPreferences.getTotalLtvMicros() / 1_000_000.0
                )
            }
        }
    }

    fun trackRewardedInterstitialImpression() {
        viewModelScope.launch {
            addSimulatedRevenue(ECPM_REWARDED_INTERSTITIAL_INR)
            userPreferences.incrementRewardedInterstitialImpressions()
            _uiState.update {
                it.copy(
                    rewardedInterstitialImpressions = userPreferences.getRewardedInterstitialImpressions(),
                    totalLtvInr = userPreferences.getTotalLtvMicros() / 1_000_000.0
                )
            }
        }
    }

    fun trackNativeImpression() {
        viewModelScope.launch {
            addSimulatedRevenue(ECPM_NATIVE_INR)
            userPreferences.incrementNativeImpressions()
            _uiState.update {
                it.copy(
                    nativeImpressions = userPreferences.getNativeImpressions(),
                    totalLtvInr = userPreferences.getTotalLtvMicros() / 1_000_000.0
                )
            }
        }
    }
}
