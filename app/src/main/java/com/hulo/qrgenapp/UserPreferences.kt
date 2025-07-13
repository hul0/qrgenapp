package com.hulo.qrgenapp

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit // Ensure this import is present if using 'edit { }' block

class UserPreferences(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "qr_gen_app_prefs"
        private const val KEY_COINS = "user_coins"
        private const val KEY_DIAMONDS = "user_diamonds" // New: Diamonds currency
        private const val KEY_IS_PREMIUM = "is_premium_user" // New: Premium status
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_REDEEMED_CODES = "redeemed_codes" // New: Track redeemed codes
        private const val KEY_SCAN_HISTORY = "scan_history" // New: Scan history

        // New keys for daily login bonus
        private const val KEY_LAST_LOGIN_DATE = "last_login_date"
        private const val KEY_DAILY_STREAK = "daily_streak"

        // Define the default starting coins and diamonds
        const val DEFAULT_STARTING_COINS = 0 // Changed to 0 as per new logic
        const val DEFAULT_STARTING_DIAMONDS = 0 // New: Default diamonds
        const val FREE_USER_HISTORY_LIMIT = 20 // Limit for free users
    }

    // Get current coin balance
    fun getCoins(): Int {
        return preferences.getInt(KEY_COINS, DEFAULT_STARTING_COINS)
    }

    // Set coin balance
    fun setCoins(coins: Int) {
        preferences.edit { putInt(KEY_COINS, coins) }
    }

    // Get current diamond balance
    fun getDiamonds(): Int {
        return preferences.getInt(KEY_DIAMONDS, DEFAULT_STARTING_DIAMONDS)
    }

    // Set diamond balance
    fun setDiamonds(diamonds: Int) {
        preferences.edit { putInt(KEY_DIAMONDS, diamonds) }
    }

    // Check if user is premium
    fun isPremium(): Boolean {
        return preferences.getBoolean(KEY_IS_PREMIUM, false)
    }

    // Set premium status
    fun setPremium(isPremium: Boolean) {
        preferences.edit { putBoolean(KEY_IS_PREMIUM, isPremium) }
    }

    // Check if it's the first launch
    fun isFirstLaunch(): Boolean {
        return preferences.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    // Mark that the app has been launched at least once
    fun setFirstLaunch(isFirst: Boolean) {
        preferences.edit().putBoolean(KEY_FIRST_LAUNCH, isFirst).apply()
    }

    // Get redeemed codes set
    fun getRedeemedCodes(): Set<String> {
        return preferences.getStringSet(KEY_REDEEMED_CODES, emptySet()) ?: emptySet()
    }

    // Add a redeemed code
    fun addRedeemedCode(codeHash: String) {
        val currentCodes = getRedeemedCodes().toMutableSet()
        currentCodes.add(codeHash)
        preferences.edit { putStringSet(KEY_REDEEMED_CODES, currentCodes) }
    }

    // Get scan history
    fun getScanHistory(): List<String> {
        val historyJson = preferences.getString(KEY_SCAN_HISTORY, null)
        return historyJson?.let {
            // Deserialize JSON string to List<String>
            it.split(";;;") // Simple delimiter for now, consider JSON parsing for complex objects
        } ?: emptyList()
    }

    // Add a scan result to history
    fun addScanToHistory(scanResult: String, isPremiumUser: Boolean) {
        val currentHistory = getScanHistory().toMutableList()
        currentHistory.add(0, scanResult) // Add to the beginning

        if (!isPremiumUser && currentHistory.size > FREE_USER_HISTORY_LIMIT) {
            // Trim history for free users
            currentHistory.subList(FREE_USER_HISTORY_LIMIT, currentHistory.size).clear()
        }
        // Serialize List<String> to JSON string for storage
        preferences.edit { putString(KEY_SCAN_HISTORY, currentHistory.joinToString(";;;")) }
    }

    // Clear scan history
    fun clearScanHistory() {
        preferences.edit { remove(KEY_SCAN_HISTORY) }
    }

    // New: Get last login date
    fun getLastLoginDate(): String? {
        return preferences.getString(KEY_LAST_LOGIN_DATE, null)
    }

    // New: Set last login date - now accepts nullable String
    fun setLastLoginDate(date: String?) { // Changed parameter to String?
        preferences.edit { putString(KEY_LAST_LOGIN_DATE, date) }
    }

    // New: Get daily streak
    fun getDailyStreak(): Int {
        return preferences.getInt(KEY_DAILY_STREAK, 0)
    }

    // New: Set daily streak
    fun setDailyStreak(streak: Int) {
        preferences.edit { putInt(KEY_DAILY_STREAK, streak) }
    }
}
