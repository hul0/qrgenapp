package com.hulo.qrgenapp
import android.util.Log

object AdEventLogger {

    // A unique tag for easy filtering in Logcat
    private const val TAG = "AdTriggerLog"

    fun logBannerLoaded() {
        Log.d(TAG, "Trigger: +1 B (Banner Loaded/Refreshed)")
    }

    fun logNativeLoaded() {
        Log.d(TAG, "Trigger: +1 N (Native Loaded)")
    }

    fun logRewardedShown() {
        Log.d(TAG, "Trigger: +1 R (Rewarded Shown)")
    }

    fun logInterstitialShown() {
        Log.d(TAG, "Trigger: +1 I (Interstitial Shown)")
    }
}