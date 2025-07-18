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
        private const val KEY_DIAMONDS = "user_diamonds"
        private const val KEY_IS_PREMIUM = "is_premium_user"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_REDEEMED_CODES = "redeemed_codes"
        private const val KEY_SCAN_HISTORY = "scan_history"
        private const val KEY_LAST_LOGIN_DATE = "last_login_date"
        private const val KEY_DAILY_STREAK = "daily_streak"
        private const val KEY_FIRST_UPDATE_WARNING_TIME = "first_update_warning_time"

        // --- LTV & Ad Analytics Keys ---
        private const val KEY_BANNER_IMPRESSIONS = "banner_impressions"
        private const val KEY_INTERSTITIAL_IMPRESSIONS = "interstitial_impressions"
        private const val KEY_REWARDED_IMPRESSIONS = "rewarded_impressions"
        private const val KEY_REWARDED_INTERSTITIAL_IMPRESSIONS = "rewarded_interstitial_impressions"
        private const val KEY_NATIVE_IMPRESSIONS = "native_impressions"
        private const val KEY_TOTAL_LTV_MICROS = "total_ltv_micros"

        const val DEFAULT_STARTING_COINS = 0
        const val DEFAULT_STARTING_DIAMONDS = 0
        const val FREE_USER_HISTORY_LIMIT = 20
    }

    // --- YOUR ORIGINAL WORKING FUNCTIONS (UNCHANGED) ---
    fun getCoins(): Int {
        return preferences.getInt(KEY_COINS, DEFAULT_STARTING_COINS)
    }

    fun setCoins(coins: Int) {
        preferences.edit { putInt(KEY_COINS, coins) }
    }

    fun getDiamonds(): Int {
        return preferences.getInt(KEY_DIAMONDS, DEFAULT_STARTING_DIAMONDS)
    }

    fun setDiamonds(diamonds: Int) {
        preferences.edit { putInt(KEY_DIAMONDS, diamonds) }
    }

    fun isPremium(): Boolean {
        return preferences.getBoolean(KEY_IS_PREMIUM, false)
    }

    fun setPremium(isPremium: Boolean) {
        preferences.edit { putBoolean(KEY_IS_PREMIUM, isPremium) }
    }

    fun isFirstLaunch(): Boolean {
        return preferences.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunch(isFirst: Boolean) {
        preferences.edit().putBoolean(KEY_FIRST_LAUNCH, isFirst).apply()
    }

    fun getRedeemedCodes(): Set<String> {
        return preferences.getStringSet(KEY_REDEEMED_CODES, emptySet()) ?: emptySet()
    }

    fun addRedeemedCode(codeHash: String) {
        val currentCodes = getRedeemedCodes().toMutableSet()
        currentCodes.add(codeHash)
        preferences.edit { putStringSet(KEY_REDEEMED_CODES, currentCodes) }
    }

    fun getScanHistory(): List<String> {
        val historyJson = preferences.getString(KEY_SCAN_HISTORY, null)
        return historyJson?.let {
            it.split(";;;")
        } ?: emptyList()
    }

    fun addScanToHistory(scanResult: String, isPremiumUser: Boolean) {
        val currentHistory = getScanHistory().toMutableList()
        currentHistory.add(0, scanResult)
        if (!isPremiumUser && currentHistory.size > FREE_USER_HISTORY_LIMIT) {
            currentHistory.subList(FREE_USER_HISTORY_LIMIT, currentHistory.size).clear()
        }
        preferences.edit { putString(KEY_SCAN_HISTORY, currentHistory.joinToString(";;;")) }
    }

    fun clearScanHistory() {
        preferences.edit { remove(KEY_SCAN_HISTORY) }
    }

    fun getLastLoginDate(): String? {
        return preferences.getString(KEY_LAST_LOGIN_DATE, null)
    }

    fun setLastLoginDate(date: String?) {
        preferences.edit { putString(KEY_LAST_LOGIN_DATE, date) }
    }

    fun getDailyStreak(): Int {
        return preferences.getInt(KEY_DAILY_STREAK, 0)
    }

    fun setDailyStreak(streak: Int) {
        preferences.edit { putInt(KEY_DAILY_STREAK, streak) }
    }

    fun getFirstUpdateWarningTime(): Long {
        return preferences.getLong(KEY_FIRST_UPDATE_WARNING_TIME, 0L)
    }

    fun setFirstUpdateWarningTime(timeMillis: Long) {
        preferences.edit { putLong(KEY_FIRST_UPDATE_WARNING_TIME, timeMillis) }
    }

    fun clearFirstUpdateWarningTime() {
        preferences.edit { remove(KEY_FIRST_UPDATE_WARNING_TIME) }
    }

    // --- NEW LTV & AD ANALYTICS FUNCTIONS (ADDED CAREFULLY) ---

    // Getters for LTV data
    fun getBannerImpressions(): Int = preferences.getInt(KEY_BANNER_IMPRESSIONS, 0)
    fun getInterstitialImpressions(): Int = preferences.getInt(KEY_INTERSTITIAL_IMPRESSIONS, 0)
    fun getRewardedImpressions(): Int = preferences.getInt(KEY_REWARDED_IMPRESSIONS, 0)
    fun getRewardedInterstitialImpressions(): Int = preferences.getInt(KEY_REWARDED_INTERSTITIAL_IMPRESSIONS, 0)
    fun getNativeImpressions(): Int = preferences.getInt(KEY_NATIVE_IMPRESSIONS, 0)
    fun getTotalLtvMicros(): Long = preferences.getLong(KEY_TOTAL_LTV_MICROS, 0L)

    // Setters for LTV data, using your working 'edit' block style
    fun incrementBannerImpressions() {
        preferences.edit { putInt(KEY_BANNER_IMPRESSIONS, getBannerImpressions() + 1) }
    }

    fun incrementInterstitialImpressions() {
        preferences.edit { putInt(KEY_INTERSTITIAL_IMPRESSIONS, getInterstitialImpressions() + 1) }
    }

    fun incrementRewardedImpressions() {
        preferences.edit { putInt(KEY_REWARDED_IMPRESSIONS, getRewardedImpressions() + 1) }
    }

    fun incrementRewardedInterstitialImpressions() {
        preferences.edit { putInt(KEY_REWARDED_INTERSTITIAL_IMPRESSIONS, getRewardedInterstitialImpressions() + 1) }
    }

    fun incrementNativeImpressions() {
        preferences.edit { putInt(KEY_NATIVE_IMPRESSIONS, getNativeImpressions() + 1) }
    }

    fun addRevenueMicros(micros: Long) {
        val currentMicros = getTotalLtvMicros()
        preferences.edit { putLong(KEY_TOTAL_LTV_MICROS, currentMicros + micros) }
    }
}
