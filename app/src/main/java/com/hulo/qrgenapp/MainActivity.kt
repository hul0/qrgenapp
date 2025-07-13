package com.hulo.qrgenapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Import Color
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.hulo.qrgenapp.ui.theme.QRGenAppTheme // Keep this import if it defines other theme aspects not related to colors

// In-app update imports
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

// IMPORTANT: Use Google's TEST Ad Unit IDs for development and testing.
// Replace these with your actual production IDs ONLY when your app is published.
private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // Google's Test Interstitial Ad Unit ID
private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917" // Google's Test Rewarded Ad Unit ID
private const val REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/5354046379" // Google's Test Rewarded Interstitial Ad Unit ID
private const val NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110" // Google's Test Native Ad Unit ID
private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111" // Google's Test Banner Ad Unit ID
private const val AD_LOG_TAG = "AdMob"
private const val APP_UPDATE_TAG = "AppUpdate"

// Constants for in-app updates
private const val MANDATORY_UPDATE_STALENESS_DAYS = 7
private const val HIGH_PRIORITY_UPDATE = 4 // Priority 4 and 5 are considered high

class MainActivity : ComponentActivity() {

    private val qrGenViewModel: QRGenViewModel by viewModels()
    private val qrScanViewModel: QRScanViewModel by viewModels()
    private lateinit var userViewModel: UserViewModel

    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd? = null
    private var mRewardedInterstitialAd: RewardedInterstitialAd? = null
    private var mNativeAd: NativeAd? = null

    private var userActionCount = 0
    private var lastInterstitialTime = 0L
    private val minInterstitialInterval = 40000L // 40 seconds
    private val actionsBeforeInterstitial = 7

    private var isInterstitialLoading = false
    private var isRewardedLoading = false
    private var isRewardedInterstitialLoading = false
    private var isNativeAdLoading = false

    // In-app update variables
    private lateinit var appUpdateManager: AppUpdateManager
    private var updateListener: InstallStateUpdatedListener? = null

    // Modern way to handle Activity results for in-app updates
    private val appUpdateResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            Log.w(APP_UPDATE_TAG, "Update flow failed! Result code: ${result.resultCode}")
            // If an immediate update is cancelled, we should force the user to update.
            Toast.makeText(this, "Update is required to continue.", Toast.LENGTH_LONG).show()
            finish() // Close the app if a mandatory update is not accepted.
        } else {
            Log.d(APP_UPDATE_TAG, "Update successful or in progress.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userPreferences = UserPreferences(applicationContext)
        userViewModel = UserViewModel(userPreferences)

        // Initialize AppUpdateManager
        appUpdateManager = AppUpdateManagerFactory.create(applicationContext)

        // Create a listener to track the state of flexible updates.
        updateListener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                // An update has been downloaded.
                Log.d(APP_UPDATE_TAG, "Flexible update downloaded. Prompting user to complete.")
                // Show a notification to the user to complete the update.
                popupSnackbarForCompleteUpdate()
            } else if (state.installStatus() == InstallStatus.FAILED) {
                Log.e(APP_UPDATE_TAG, "Update failed with error code: ${state.installErrorCode()}")
            }
        }
        appUpdateManager.registerListener(updateListener!!)

        // Check for app updates on launch
        checkForAppUpdates()

        MobileAds.initialize(this) {
            Log.d(AD_LOG_TAG, "Mobile Ads SDK Initialized.")
            loadInterstitialAd()
            loadRewardedAd()
            loadRewardedInterstitialAd()
            loadNativeAd()
        }

        setContent {
            var darkTheme by remember { mutableStateOf(false) }
            val userUiState by userViewModel.uiState.collectAsState()

            QRGenAppTheme(darkTheme = darkTheme) {
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF6200EE)
                ) {
                    MainAppScreen(
                        qrGenViewModel = qrGenViewModel,
                        qrScanViewModel = qrScanViewModel,
                        userViewModel = userViewModel,
                        onShowInterstitialAd = {
                            if (!userUiState.isPremium) {
                                userActionCount++
                                showInterstitialAdSmart()
                            }
                        },
                        onShowRewardedAd = ::showRewardedAd,
                        onShowRewardedInterstitialAdForDailyBonus = ::showRewardedInterstitialAdForDailyBonus,
                        darkTheme = darkTheme,
                        onToggleTheme = { darkTheme = !darkTheme },
                        nativeAd = mNativeAd,
                        isPremiumUser = userUiState.isPremium
                    )
                }
            }
        }
    }

    private fun checkForAppUpdates() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            val isUpdateAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            if (isUpdateAvailable) {
                // Determine if the update is mandatory (immediate)
                val stalenessDays = appUpdateInfo.clientVersionStalenessDays() ?: 0
                val isImmediateUpdate = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) &&
                        (appUpdateInfo.updatePriority() >= HIGH_PRIORITY_UPDATE || stalenessDays >= MANDATORY_UPDATE_STALENESS_DAYS)

                if (isImmediateUpdate) {
                    Log.d(APP_UPDATE_TAG, "Starting IMMEDIATE update. Staleness: $stalenessDays days, Priority: ${appUpdateInfo.updatePriority()}")
                    startUpdateFlow(appUpdateInfo, AppUpdateType.IMMEDIATE)
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    Log.d(APP_UPDATE_TAG, "Starting FLEXIBLE update.")
                    startUpdateFlow(appUpdateInfo, AppUpdateType.FLEXIBLE)
                }
            } else {
                Log.d(APP_UPDATE_TAG, "No update available.")
            }
        }.addOnFailureListener { e ->
            Log.e(APP_UPDATE_TAG, "App update check failed: ${e.message}")
        }
    }

    private fun startUpdateFlow(appUpdateInfo: AppUpdateInfo, type: Int) {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            appUpdateResultLauncher,
            AppUpdateOptions.newBuilder(type).build()
        )
    }

    private fun popupSnackbarForCompleteUpdate() {
        // A simple Toast is used here. In a real app, a Snackbar with an "RESTART" action is recommended.
        Toast.makeText(
            this,
            "An update has just been downloaded.",
            Toast.LENGTH_LONG
        ).apply {
            // In a real app, you'd have a button in the UI that calls appUpdateManager.completeUpdate()
            // For simplicity, we'll complete it programmatically here after a short delay or on next resume.
            view?.setOnClickListener {
                appUpdateManager.completeUpdate()
            }
            show()
        }
        // As a fallback, you can also force the update on the next resume.
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            // If an immediate update is in progress, resume it.
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                Log.d(APP_UPDATE_TAG, "Resuming IMMEDIATE update flow.")
                startUpdateFlow(appUpdateInfo, AppUpdateType.IMMEDIATE)
            }
            // If a flexible update has been downloaded, complete the installation.
            else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                Log.d(APP_UPDATE_TAG, "Flexible update downloaded on resume. Completing update.")
                popupSnackbarForCompleteUpdate()
            }
        }
    }

    override fun onDestroy() {
        updateListener?.let {
            appUpdateManager.unregisterListener(it)
        }
        mNativeAd?.destroy()
        super.onDestroy()
    }

    // --- Ad Loading and Showing Logic (Unchanged) ---
    private fun loadInterstitialAd() {
        if (isInterstitialLoading || mInterstitialAd != null) return
        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, INTERSTITIAL_AD_UNIT_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(AD_LOG_TAG, "Interstitial ad failed to load: ${adError.message}")
                mInterstitialAd = null
                isInterstitialLoading = false
            }
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d(AD_LOG_TAG, "Interstitial ad loaded successfully.")
                mInterstitialAd = interstitialAd
                isInterstitialLoading = false
                setInterstitialAdCallbacks()
            }
        })
    }

    private fun setInterstitialAdCallbacks() {
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                mInterstitialAd = null
                lastInterstitialTime = System.currentTimeMillis()
                userActionCount = 0
                loadInterstitialAd()
            }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                mInterstitialAd = null
                loadInterstitialAd()
            }
            override fun onAdShowedFullScreenContent() {}
        }
    }

    private fun showInterstitialAdSmart() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastAd = currentTime - lastInterstitialTime
        if (userActionCount >= actionsBeforeInterstitial && timeSinceLastAd >= minInterstitialInterval && mInterstitialAd != null) {
            mInterstitialAd?.show(this)
        } else {
            if (mInterstitialAd == null && !isInterstitialLoading) {
                loadInterstitialAd()
            }
        }
    }

    private fun loadRewardedAd() {
        if (isRewardedLoading || mRewardedAd != null) return
        isRewardedLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, REWARDED_AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mRewardedAd = null
                isRewardedLoading = false
            }
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                mRewardedAd = rewardedAd
                isRewardedLoading = false
                setRewardedAdCallbacks()
            }
        })
    }

    private fun setRewardedAdCallbacks() {
        mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                mRewardedAd = null
                loadRewardedAd()
            }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                mRewardedAd = null
                loadRewardedAd()
            }
            override fun onAdShowedFullScreenContent() {}
        }
    }

    private fun showRewardedAd(onRewardEarned: (Int) -> Unit = {}) {
        if (mRewardedAd != null) {
            mRewardedAd?.show(this) { rewardItem ->
                userViewModel.addCoins(50)
                onRewardEarned(rewardItem.amount)
            }
        } else {
            if (!isRewardedLoading) {
                loadRewardedAd()
            }
        }
    }

    private fun loadRewardedInterstitialAd() {
        if (isRewardedInterstitialLoading || mRewardedInterstitialAd != null) return
        isRewardedInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedInterstitialAd.load(this, REWARDED_INTERSTITIAL_AD_UNIT_ID, adRequest, object : RewardedInterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mRewardedInterstitialAd = null
                isRewardedInterstitialLoading = false
            }
            override fun onAdLoaded(ad: RewardedInterstitialAd) {
                mRewardedInterstitialAd = ad
                isRewardedInterstitialLoading = false
                setRewardedInterstitialAdCallbacks()
            }
        })
    }

    private fun setRewardedInterstitialAdCallbacks() {
        mRewardedInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                mRewardedInterstitialAd = null
                loadRewardedInterstitialAd()
            }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                mRewardedInterstitialAd = null
                loadRewardedInterstitialAd()
            }
            override fun onAdShowedFullScreenContent() {}
        }
    }

    private fun showRewardedInterstitialAdForDailyBonus(onRewardEarned: (Boolean) -> Unit) {
        if (mRewardedInterstitialAd != null) {
            mRewardedInterstitialAd?.show(this) {
                onRewardEarned(true)
            }
        } else {
            if (!isRewardedInterstitialLoading) {
                loadRewardedInterstitialAd()
            }
            onRewardEarned(false)
        }
    }

    private fun loadNativeAd() {
        if (isNativeAdLoading || mNativeAd != null) return
        isNativeAdLoading = true
        val adLoader = AdLoader.Builder(this, NATIVE_AD_UNIT_ID)
            .forNativeAd { nativeAd ->
                mNativeAd = nativeAd
                isNativeAdLoading = false
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mNativeAd = null
                    isNativeAdLoading = false
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }
}

// --- Composable Screens (Unchanged) ---

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Generate : Screen("generate", "Generate", Icons.Default.Create)
    object Scan : Screen("scan", "Scan", Icons.Default.QrCodeScanner)
    object GainCoins : Screen("gain_coins", "Coins", Icons.Default.MonetizationOn)
    object Redeem : Screen("redeem", "Redeem", Icons.Default.CardGiftcard)
    object History : Screen("history", "History", Icons.Default.History)
    object About : Screen("about", "About", Icons.Default.Info)
    object Premium : Screen("premium", "Premium", Icons.Default.Star)
}

val navItems = listOf(Screen.Home, Screen.Generate, Screen.Scan, Screen.GainCoins, Screen.Redeem, Screen.History, Screen.About)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    qrGenViewModel: QRGenViewModel,
    qrScanViewModel: QRScanViewModel,
    userViewModel: UserViewModel,
    onShowInterstitialAd: () -> Unit,
    onShowRewardedAd: (onRewardEarned: (Int) -> Unit) -> Unit,
    onShowRewardedInterstitialAdForDailyBonus: (onRewardEarned: (Boolean) -> Unit) -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    nativeAd: NativeAd?,
    isPremiumUser: Boolean
) {
    val navController = rememberNavController()
    val userUiState by userViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val Purple500 = Color(0xFF6200EE)
    val Purple700 = Color(0xFF3700B3)
    val White = Color(0xFFFFFFFF)
    val LightGray = Color(0xFFF0F0F0)
    val DarkPurple = Color(0xFF4A00A0)
    val MediumPurple = Color(0xFF8A2BE2)

    var showDailyBonusDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userUiState.dailyBonusAvailable) {
        if (userUiState.dailyBonusAvailable) {
            showDailyBonusDialog = true
        }
    }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect {
            onShowInterstitialAd()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QRWiz", style = MaterialTheme.typography.titleLarge, color = White) },
                navigationIcon = {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    if (currentRoute != Screen.Home.route) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Purple700,
                    titleContentColor = White
                ),
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coin Balance",
                            tint = LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = userUiState.coins.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = "Diamond Balance",
                            tint = LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = userUiState.diamonds.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = Icons.Default.LightMode,
                            contentDescription = "Toggle Dark Mode",
                            tint = White
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column {
                if (!isPremiumUser) {
                    BannerAd(adUnitId = BANNER_AD_UNIT_ID)
                }
                NavigationBar(
                    containerColor = Purple700
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    navItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title, modifier = Modifier.size(24.dp)) },
                            label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                selectedIconColor = White,
                                selectedTextColor = White,
                                unselectedIconColor = LightGray,
                                unselectedTextColor = LightGray,
                                indicatorColor = MediumPurple
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            coinBalance = userUiState.coins,
                            diamondBalance = userUiState.diamonds,
                            isPremiumUser = isPremiumUser,
                            onNavigateToGenerate = { navController.navigate(Screen.Generate.route) },
                            onNavigateToScan = { navController.navigate(Screen.Scan.route) },
                            onNavigateToGainCoins = { navController.navigate(Screen.GainCoins.route) },
                            onNavigateToRedeem = { navController.navigate(Screen.Redeem.route) },
                            onNavigateToHistory = { navController.navigate(Screen.History.route) },
                            onNavigateToPremium = { navController.navigate(Screen.Premium.route) },
                            nativeAd = nativeAd,
                            showNativeAd = !isPremiumUser
                        )
                    }
                    composable(Screen.Generate.route) {
                        QRGenScreen(
                            viewModel = qrGenViewModel,
                            coinBalance = userUiState.coins,
                            onDeductCoins = userViewModel::deductCoins,
                            onShowInterstitialAd = onShowInterstitialAd,
                            showToast = { message -> context.showToast(message) },
                            nativeAd = nativeAd,
                            showNativeAd = !isPremiumUser
                        )
                    }
                    composable(Screen.Scan.route) {
                        QRScanScreen(
                            viewModel = qrScanViewModel,
                            onAddCoins = { amount ->
                                userViewModel.addCoins(amount)
                            },
                            onAddScanToHistory = userViewModel::addScanToHistory,
                            onShowInterstitialAd = onShowInterstitialAd,
                            isPremiumUser = isPremiumUser,
                            showToast = { message -> context.showToast(message) },
                            nativeAd = nativeAd,
                            showAd = !isPremiumUser
                        )
                    }
                    composable(Screen.GainCoins.route) {
                        GainCoinsScreen(
                            coinBalance = userUiState.coins,
                            onShowRewardedAd = onShowRewardedAd,
                            onNavigateToScan = { navController.navigate(Screen.Scan.route) },
                            onNavigateToPremium = { navController.navigate(Screen.Premium.route) },
                            nativeAd = nativeAd,
                            showNativeAd = !isPremiumUser ,
                            isPremiumUser = isPremiumUser,
                            dailyBonusAvailable = userUiState.dailyBonusAvailable,
                            dailyBonusAmount = userUiState.dailyBonusAmount,
                            dailyStreak = userUiState.dailyStreak,
                            dailyBonusPattern = userUiState.dailyBonusPattern,
                            onClaimDailyBonus = {
                                onShowRewardedInterstitialAdForDailyBonus { adWatchedAndRewarded ->
                                    if (adWatchedAndRewarded) {
                                        userViewModel.claimDailyBonus()
                                    } else {
                                        context.showToast("Ad not ready or not watched. Please try again.")
                                    }
                                }
                            }
                        )
                    }
                    composable(Screen.Redeem.route) {
                        RedeemCodeScreen(
                            userViewModel = userViewModel,
                            showToast = { message -> context.showToast(message) },
                            nativeAd = nativeAd,
                            showAd = !isPremiumUser
                        )
                    }
                    composable(Screen.History.route) {
                        HistoryScreen(
                            userViewModel = userViewModel,
                            onNavigateToPremium = { navController.navigate(Screen.Premium.route) },
                            showToast = { message -> context.showToast(message) },
                            onShowInterstitialAd = onShowInterstitialAd,
                            nativeAd = nativeAd,
                            showAd = !isPremiumUser
                        )
                    }
                    composable(Screen.About.route) {
                        AboutScreen(
                            nativeAd = nativeAd,
                            showNativeAd = !isPremiumUser
                        )
                    }
                    composable(Screen.Premium.route) {
                        PremiumPlanScreen(
                            userViewModel = userViewModel,
                            showToast = { message -> context.showToast(message) },
                            nativeAd = nativeAd,
                            showAd = !isPremiumUser
                        )
                    }
                }
            }
        }
    }

    if (showDailyBonusDialog && userUiState.dailyBonusAvailable) {
        AlertDialog(
            onDismissRequest = { showDailyBonusDialog = false },
            icon = { Icon(Icons.Default.CardGiftcard, contentDescription = "Daily Bonus", tint = White) },
            title = { Text("Daily Login Bonus!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = White) },
            text = {
                Column {
                    Text(
                        "Claim your daily bonus of ${userUiState.dailyBonusAmount} coins!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = LightGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Current streak: ${userUiState.dailyStreak + 1} days.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LightGray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onShowRewardedInterstitialAdForDailyBonus { adWatchedAndRewarded ->
                            if (adWatchedAndRewarded) {
                                userViewModel.claimDailyBonus()
                                showDailyBonusDialog = false
                            } else {
                                context.showToast("Ad not ready or not watched. Please try again.")
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MediumPurple)
                ) {
                    Text("Claim Now!", color = White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDailyBonusDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                ) {
                    Text("Later", color = LightGray)
                }
            }
        )
    }
}

@Composable
fun BannerAd(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color(0xFF3700B3))
            .padding(vertical = 4.dp),
        factory = {
            AdView(context).apply {
                setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                        context,
                        AdSize.FULL_WIDTH
                    )
                )
                setAdUnitId(adUnitId)
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d(AD_LOG_TAG, "Banner ad loaded successfully for unit: $adUnitId")
                    }
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e(AD_LOG_TAG, "Banner ad failed to load for unit $adUnitId: ${adError.message}")
                    }
                    override fun onAdClicked() {}
                }
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            adView.loadAd(AdRequest.Builder().build())
        }
    )
}

@Composable
fun NativeAdViewComposable(
    nativeAd: NativeAd?,
    modifier: Modifier = Modifier,
    showAd: Boolean = true
) {
    if (!showAd || nativeAd == null) {
        Spacer(modifier = Modifier.height(0.dp))
        return
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable {
                nativeAd.performClick(Bundle())
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4A00A0)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF6200EE))
        ) {
            Text(
                text = "Sponsored",
                style = MaterialTheme.typography.labelSmall,
                color = LightGray,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    factory = { context ->
                        val adView = AdView(context).apply {
                            setAdSize(AdSize.MEDIUM_RECTANGLE)
                            setAdUnitId(NATIVE_AD_UNIT_ID)
                            loadAd(AdRequest.Builder().build())
                        }
                        adView
                    },
                    update = { adView ->
                        adView.loadAd(AdRequest.Builder().build())
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = nativeAd.headline ?: "Ad Headline",
                    style = MaterialTheme.typography.titleMedium,
                    color = White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = nativeAd.body ?: "Ad Body Text",
                    style = MaterialTheme.typography.bodySmall,
                    color = LightGray,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun RewardedAdButton(
    onShowRewardedAd: (onRewardEarned: (Int) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Watch Ad for Premium Features"
) {
    var rewardEarned by remember { mutableStateOf(0) }
    OutlinedButton(
        onClick = {
            onShowRewardedAd { reward ->
                rewardEarned += reward
            }
        },
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = White)
        if (rewardEarned > 0) {
            Text(" (Rewards: $rewardEarned)", color = Color(193, 77, 238, 255), style = MaterialTheme.typography.labelLarge)
        }
    }
}

