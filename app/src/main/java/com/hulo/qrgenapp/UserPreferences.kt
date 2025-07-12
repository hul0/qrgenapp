// file: UserPreferences.kt
package com.hulo.qrgenapp

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class UserPreferences(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "qr_gen_app_prefs"
        private const val KEY_COINS = "user_coins"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        // Define the default starting coins here for consistency, though ViewModel will use it on first launch
        const val DEFAULT_STARTING_COINS = 0
    }

    // Get current coin balance
    fun getCoins(): Int {
        // Returns 0 if KEY_COINS is not found. The first_launch logic in ViewModel will handle setting it to 10.
        return preferences.getInt(KEY_COINS, 0)
    }

    // Set coin balance
    fun setCoins(coins: Int) {
        preferences.edit { putInt(KEY_COINS, coins) }
    }

    // Check if it's the first launch
    fun isFirstLaunch(): Boolean {
        // Default value is true, so it will be true if not set yet
        return preferences.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    // Mark that the app has been launched at least once
    fun setFirstLaunch(isFirst: Boolean) {
        preferences.edit().putBoolean(KEY_FIRST_LAUNCH, isFirst).apply()
    }
}