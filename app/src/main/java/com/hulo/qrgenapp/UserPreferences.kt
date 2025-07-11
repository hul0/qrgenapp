package com.hulo.qrgenapp

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "qr_gen_app_prefs"
        private const val KEY_COINS = "user_coins"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }

    // Get current coin balance
    fun getCoins(): Int {
        return preferences.getInt(KEY_COINS, 0)
    }

    // Set coin balance
    fun setCoins(coins: Int) {
        preferences.edit().putInt(KEY_COINS, coins).apply()
    }

    // Check if it's the first launch
    fun isFirstLaunch(): Boolean {
        return preferences.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    // Mark that the app has been launched at least once
    fun setFirstLaunch(isFirst: Boolean) {
        preferences.edit().putBoolean(KEY_FIRST_LAUNCH, isFirst).apply()
    }
}
