package com.hulo.qrgenapp

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button as AndroidButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.Color.Companion.Yellow
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
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.hulo.qrgenapp.ui.theme.QRGenAppTheme
import java.util.*

private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
private const val REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/5354046379"
private const val NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
private const val AD_LOG_TAG = "AdMob"
private const val APP_UPDATE_TAG = "AppUpdate"
private const val MANDATORY_UPDATE_STALENESS_DAYS = 7
private const val HIGH_PRIORITY_UPDATE = 4
private const val NATIVE_AD_CACHE_SIZE = 3

class MainActivity : ComponentActivity() {

    private val qrGenViewModel: QRGenViewModel by viewModels()
    private val qrScanViewModel: QRScanViewModel by viewModels()
    private lateinit var userViewModel: UserViewModel

    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd? = null
    private var mRewardedInterstitialAd: RewardedInterstitialAd? = null

    private val nativeAdCache = Collections.synchronizedList(mutableListOf<NativeAd>())
    private var currentNativeAd by mutableStateOf<NativeAd?>(null)
    private var nativeAdsLoadingCount = 0

    private var userActionCount = 0
    private var lastInterstitialTime = 0L
    private val minInterstitialInterval = 40000L
    private val actionsBeforeInterstitial = 5

    private var isInterstitialLoading = false
    private var isRewardedLoading = false
    private var isRewardedInterstitialLoading = false

    private lateinit var appUpdateManager: AppUpdateManager
    private var updateListener: InstallStateUpdatedListener? = null

    private val appUpdateResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            Log.w(APP_UPDATE_TAG, "Update flow failed! Result code: ${result.resultCode}")
            Toast.makeText(this, "Update is required to continue.", Toast.LENGTH_LONG).show()
            finish()
        } else {
            Log.d(APP_UPDATE_TAG, "Update successful or in progress.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userPreferences = UserPreferences(applicationContext)
        userViewModel = UserViewModel(userPreferences)

        appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
        updateListener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                popupSnackbarForCompleteUpdate()
            } else if (state.installStatus() == InstallStatus.FAILED) {
                Log.e(APP_UPDATE_TAG, "Update failed with error code: ${state.installErrorCode()}")
            }
        }
        appUpdateManager.registerListener(updateListener!!)
        checkForAppUpdates()

        MobileAds.initialize(this) {
            Log.d(AD_LOG_TAG, "Mobile Ads SDK Initialized.")
            loadInterstitialAd()
            loadRewardedAd()
            loadRewardedInterstitialAd()
            fillNativeAdCache()
            refreshCurrentAdFromCache()
        }

        setContent {
            val userUiState by userViewModel.uiState.collectAsState()

            QRGenAppTheme(darkTheme = false) {
                Surface(
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
                        darkTheme = false,
                        onToggleTheme = {},
                        nativeAd = currentNativeAd,
                        isPremiumUser = userUiState.isPremium,
                        onRefreshNativeAd = ::refreshCurrentAdFromCache
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
                val stalenessDays = appUpdateInfo.clientVersionStalenessDays() ?: 0
                val isImmediateUpdate = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) &&
                        (appUpdateInfo.updatePriority() >= HIGH_PRIORITY_UPDATE || stalenessDays >= MANDATORY_UPDATE_STALENESS_DAYS)
                if (isImmediateUpdate) {
                    startUpdateFlow(appUpdateInfo, AppUpdateType.IMMEDIATE)
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    startUpdateFlow(appUpdateInfo, AppUpdateType.FLEXIBLE)
                }
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
        Toast.makeText(this, "An update has been downloaded.", Toast.LENGTH_LONG).apply {
            view?.setOnClickListener { appUpdateManager.completeUpdate() }
            show()
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startUpdateFlow(appUpdateInfo, AppUpdateType.IMMEDIATE)
            } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                popupSnackbarForCompleteUpdate()
            }
        }
    }

    override fun onDestroy() {
        updateListener?.let { appUpdateManager.unregisterListener(it) }
        nativeAdCache.forEach { it.destroy() }
        nativeAdCache.clear()
        currentNativeAd?.destroy()
        super.onDestroy()
    }

    private fun loadInterstitialAd() {
        if (isInterstitialLoading || mInterstitialAd != null) return
        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, INTERSTITIAL_AD_UNIT_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
                isInterstitialLoading = false
            }
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
                isInterstitialLoading = false
                mInterstitialAd?.onPaidEventListener = OnPaidEventListener { adValue ->
                    Log.d(AD_LOG_TAG, "Interstitial ad paid: ${adValue.valueMicros} ${adValue.currencyCode}")
                    userViewModel.trackInterstitialImpression()
                }
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
            override fun onAdImpression() {
                super.onAdImpression()
                Log.d(AD_LOG_TAG, "Interstitial ad impression recorded.")
                userViewModel.trackInterstitialImpression()
            }
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
            override fun onAdFailedToLoad(adError: LoadAdError) { mRewardedAd = null; isRewardedLoading = false }
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                mRewardedAd = rewardedAd
                isRewardedLoading = false
                mRewardedAd?.onPaidEventListener = OnPaidEventListener { adValue ->
                    Log.d(AD_LOG_TAG, "Rewarded ad paid: ${adValue.valueMicros} ${adValue.currencyCode}")
                    userViewModel.trackRewardedImpression()
                }
                setRewardedAdCallbacks()
            }
        })
    }

    private fun setRewardedAdCallbacks() {
        mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { mRewardedAd = null; loadRewardedAd() }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) { mRewardedAd = null; loadRewardedAd() }
            override fun onAdShowedFullScreenContent() {}
            override fun onAdImpression() {
                super.onAdImpression()
                Log.d(AD_LOG_TAG, "Rewarded ad impression recorded.")
                userViewModel.trackRewardedImpression()
            }
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
            override fun onAdFailedToLoad(adError: LoadAdError) { mRewardedInterstitialAd = null; isRewardedInterstitialLoading = false }
            override fun onAdLoaded(ad: RewardedInterstitialAd) {
                mRewardedInterstitialAd = ad
                isRewardedInterstitialLoading = false
                mRewardedInterstitialAd?.onPaidEventListener = OnPaidEventListener { adValue ->
                    Log.d(AD_LOG_TAG, "Rewarded Interstitial ad paid: ${adValue.valueMicros} ${adValue.currencyCode}")
                    userViewModel.trackRewardedInterstitialImpression()
                }
                setRewardedInterstitialAdCallbacks()
            }
        })
    }

    private fun setRewardedInterstitialAdCallbacks() {
        mRewardedInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { mRewardedInterstitialAd = null; loadRewardedInterstitialAd() }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) { mRewardedInterstitialAd = null; loadRewardedInterstitialAd() }
            override fun onAdShowedFullScreenContent() {}
            override fun onAdImpression() {
                super.onAdImpression()
                Log.d(AD_LOG_TAG, "Rewarded Interstitial ad impression recorded.")
                userViewModel.trackRewardedInterstitialImpression()
            }
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

    private fun fillNativeAdCache() {
        val adsToLoad = NATIVE_AD_CACHE_SIZE - nativeAdCache.size - nativeAdsLoadingCount
        if (adsToLoad <= 0) {
            return
        }
        nativeAdsLoadingCount += adsToLoad
        for (i in 1..adsToLoad) {
            val adLoader = AdLoader.Builder(this, NATIVE_AD_UNIT_ID)
                .forNativeAd { nativeAd ->
                    nativeAdCache.add(nativeAd)
                    nativeAdsLoadingCount--
                    if (currentNativeAd == null) {
                        refreshCurrentAdFromCache()
                    }
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        nativeAdsLoadingCount--
                        Log.e(AD_LOG_TAG, "Native ad failed to load for cache: ${adError.message}")
                    }
                    override fun onAdImpression() {
                        super.onAdImpression()
                        userViewModel.trackNativeImpression()
                    }
                })
                .withNativeAdOptions(NativeAdOptions.Builder().build())
                .build()
            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    private fun refreshCurrentAdFromCache() {
        currentNativeAd?.destroy()
        currentNativeAd = null
        synchronized(nativeAdCache) {
            if (nativeAdCache.isNotEmpty()) {
                currentNativeAd = nativeAdCache.removeAt(0)
            }
        }
        fillNativeAdCache()
    }
}

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
    isPremiumUser: Boolean,
    onRefreshNativeAd: () -> Unit
) {
    val navController = rememberNavController()
    val userUiState by userViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val Purple700 = Color(0xFF3700B3)
    val MediumPurple = Color(0xFF8A2BE2)

    var showDailyBonusDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userUiState.dailyBonusAvailable) {
        if (userUiState.dailyBonusAvailable) {
            showDailyBonusDialog = true
        }
    }

    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, _, _ ->
            onRefreshNativeAd()
        }
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Purple700),
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(Icons.Default.TrendingUp, "LTV", tint = White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "₹${String.format(Locale.US, "%.4f", userUiState.totalLtvInr)} | ${userUiState.interstitialImpressions} | ${userUiState.nativeImpressions} | ${userUiState.bannerImpressions} | ${userUiState.rewardedImpressions} | ${userUiState.rewardedInterstitialImpressions}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Normal,
                            color = White
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(Icons.Default.MonetizationOn, "Coin Balance", tint = Yellow, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(userUiState.coins.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = White)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(Icons.Default.Diamond, "Diamond Balance", tint = Color(37, 243, 225, 255), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(userUiState.diamonds.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = White)
                    }
                }
            )
        },
        bottomBar = {
            Column {
                if (!isPremiumUser) {
                    BannerAd(adUnitId = BANNER_AD_UNIT_ID, userViewModel = userViewModel)
                }
                NavigationBar(containerColor = Purple700) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    navItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, screen.title, modifier = Modifier.size(24.dp)) },
                            label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
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
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding).fillMaxSize()
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
                    onAddCoins = { amount -> userViewModel.addCoins(amount) },
                    onAddScanToHistory = userViewModel::addScanToHistory,
                    onShowInterstitialAd = onShowInterstitialAd,
                    isPremiumUser = isPremiumUser,
                    showToast = { message -> context.showToast(message) },
                    nativeAd = nativeAd,
                    showAd = !isPremiumUser,
                    onRefreshNativeAd = onRefreshNativeAd
                )
            }
            composable(Screen.GainCoins.route) {
                GainCoinsScreen(
                    coinBalance = userUiState.coins,
                    onShowRewardedAd = onShowRewardedAd,
                    onNavigateToScan = { navController.navigate(Screen.Scan.route) },
                    onNavigateToPremium = { navController.navigate(Screen.Premium.route) },
                    nativeAd = nativeAd,
                    showNativeAd = !isPremiumUser,
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

    if (showDailyBonusDialog && userUiState.dailyBonusAvailable) {
        AlertDialog(
            onDismissRequest = { showDailyBonusDialog = false },
            icon = { Icon(Icons.Default.CardGiftcard, "Daily Bonus", tint = Black) },
            title = { Text("Daily Login Bonus!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Black) },
            text = {
                Column {
                    Text("Claim your daily bonus of ${userUiState.dailyBonusAmount} coins!", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(4.dp))
                    Text("Current streak: ${userUiState.dailyStreak + 1} days.", style = MaterialTheme.typography.bodyMedium, color = Black)
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
                ) { Text("Claim Now!", color = White) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDailyBonusDialog = false }, shape = RoundedCornerShape(10.dp)) {
                    Text("Later", color = Black)
                }
            }
        )
    }
}

@Composable
fun BannerAd(adUnitId: String, userViewModel: UserViewModel, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, AdSize.FULL_WIDTH))
                setAdUnitId(adUnitId)
                onPaidEventListener = OnPaidEventListener { adValue ->
                    Log.d(AD_LOG_TAG, "Banner ad paid: ${adValue.valueMicros} ${adValue.currencyCode}")
                    userViewModel.trackBannerImpression()
                }
                adListener = object : AdListener() {
                    override fun onAdImpression() {
                        super.onAdImpression()
                        Log.d(AD_LOG_TAG, "Banner ad impression recorded.")
                        userViewModel.trackBannerImpression()
                    }
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e(AD_LOG_TAG, "Banner ad failed to load: ${adError.message}")
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
fun NativeAdViewComposable(
    nativeAd: NativeAd?,
    modifier: Modifier = Modifier,
    showAd: Boolean = true
) {
    if (!showAd) {
        return
    }

    // A Card with a transparent background and no elevation.
    Card(
        modifier = modifier.fillMaxWidth().fillMaxSize(),
        shape = RoundedCornerShape(0.dp), // Shape is kept to round the media view if it's the first item.
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // No shadow.
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .aspectRatio(16f / 9f)
        ) {
            if (nativeAd == null) {
                // Loading State
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // The loading indicator remains visible on the transparent background.
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Loading Ad...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Ad content with its own background set to transparent in the XML.
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        LayoutInflater.from(context)
                            .inflate(R.layout.ad_unified, null) as NativeAdView
                    },
                    update = { adView ->
                        // This logic remains unchanged.
                        val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
                        val bodyView = adView.findViewById<TextView>(R.id.ad_body)
                        val callToActionView = adView.findViewById<AndroidButton>(R.id.ad_call_to_action)
                        val iconView = adView.findViewById<ImageView>(R.id.ad_app_icon)
                        val advertiserView = adView.findViewById<TextView>(R.id.ad_advertiser)
                        val mediaView = adView.findViewById<MediaView>(R.id.ad_media)

                        adView.setNativeAd(nativeAd)
                        adView.mediaView = mediaView
                        adView.headlineView = headlineView
                        adView.bodyView = bodyView
                        adView.callToActionView = callToActionView
                        adView.iconView = iconView
                        adView.advertiserView = advertiserView

                        headlineView.text = nativeAd.headline
                        mediaView.mediaContent = nativeAd.mediaContent
                        bodyView.text = nativeAd.body
                        callToActionView.text = nativeAd.callToAction

                        if (nativeAd.icon == null) {
                            iconView.visibility = View.GONE
                        } else {
                            iconView.setImageDrawable(nativeAd.icon?.drawable)
                            iconView.visibility = View.VISIBLE
                        }

                        if (nativeAd.advertiser == null) {
                            advertiserView.visibility = View.INVISIBLE
                        } else {
                            advertiserView.text = nativeAd.advertiser
                            advertiserView.visibility = View.VISIBLE
                        }
                    }
                )

                // The "Ad" badge remains visible.
                Text(
                    text = "Ad",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

