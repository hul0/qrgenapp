package com.hulo.qrgenapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.Surface
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
import com.hulo.qrgenapp.ui.theme.QRGenAppTheme

// IMPORTANT: Use Google's TEST Ad Unit IDs for development and testing.
// Replace these with your actual production IDs ONLY when your app is published.
// Removed BANNER_AD_UNIT_ID_TOP and BANNER_AD_UNIT_ID_BOTTOM as banners will be placed per screen.
private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // Google's Test Interstitial Ad Unit ID
private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917" // Google's Test Rewarded Ad Unit ID
private const val REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/5354046379" // Google's Test Rewarded Interstitial Ad Unit ID
private const val NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110" // Google's Test Native Ad Unit ID
private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111" // Google's Test Banner Ad Unit ID
private const val AD_LOG_TAG = "AdMob"

class MainActivity : ComponentActivity() {

    private val qrGenViewModel: QRGenViewModel by viewModels()
    private val qrScanViewModel: QRScanViewModel by viewModels()
    private lateinit var userViewModel: UserViewModel // Initialize UserViewModel later with context

    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd? = null
    private var mRewardedInterstitialAd: RewardedInterstitialAd? = null // New: Rewarded Interstitial Ad
    private var mNativeAd: NativeAd? = null // New: Native Ad instance

    // Declared userActionCount here
    private var userActionCount = 0
    private var lastInterstitialTime = 0L
    private val minInterstitialInterval = 40000L // 5 seconds minimum between interstitials
    private val actionsBeforeInterstitial = 7 // Show interstitial after 5 user actions (screen switches)

    private var isInterstitialLoading = false
    private var isRewardedLoading = false
    private var isRewardedInterstitialLoading = false // New: Rewarded Interstitial loading state
    private var isNativeAdLoading = false // New: Native ad loading state

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize UserPreferences and UserViewModel
        val userPreferences = UserPreferences(applicationContext)
        userViewModel = UserViewModel(userPreferences)

        MobileAds.initialize(this) { initializationStatus ->
            Log.d(AD_LOG_TAG, "Mobile Ads SDK Initialized: $initializationStatus")
            loadInterstitialAd()
            AdEventLogger.logInterstitialShown()
            loadRewardedAd()
            loadRewardedInterstitialAd() // Load rewarded interstitial ad on app start
            loadNativeAd() // Load native ad on app start
        }

        setContent {
            var darkTheme by remember { mutableStateOf(false) } // State for dark mode toggle
            val userUiState by userViewModel.uiState.collectAsState() // Observe premium status


            QRGenAppTheme(darkTheme = darkTheme) { // Apply the custom theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppScreen(
                        qrGenViewModel = qrGenViewModel,
                        qrScanViewModel = qrScanViewModel,
                        userViewModel = userViewModel, // Pass UserViewModel
                        onShowInterstitialAd = {
                            // Only show interstitial if not premium
                            if (!userUiState.isPremium) {
                                userActionCount++
                                showInterstitialAdSmart()
                            }
                        },
                        onShowRewardedAd = ::showRewardedAd,
                        onShowRewardedInterstitialAdForDailyBonus = ::showRewardedInterstitialAdForDailyBonus, // Pass new ad function
                        darkTheme = darkTheme,
                        onToggleTheme = { darkTheme = !darkTheme }, // Pass toggle function
                        nativeAd = mNativeAd, // Pass native ad to composable
                        isPremiumUser = userUiState.isPremium // Pass premium status
                    )
                }
            }
        }
    }

    private fun loadInterstitialAd() {
        if (isInterstitialLoading || mInterstitialAd != null) return

        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            this,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(AD_LOG_TAG, "Interstitial ad failed to load: ${adError.message} (Code: ${adError.code})")
                    mInterstitialAd = null
                    isInterstitialLoading = false
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(AD_LOG_TAG, "Interstitial ad loaded successfully.")
                    AdEventLogger.logInterstitialShown()
                    mInterstitialAd = interstitialAd
                    isInterstitialLoading = false
                    setInterstitialAdCallbacks()
                }
            }
        )
    }

    private fun setInterstitialAdCallbacks() {
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(AD_LOG_TAG, "Interstitial ad was dismissed.")
                mInterstitialAd = null
                lastInterstitialTime = System.currentTimeMillis()
                userActionCount = 0 // Reset after showing an ad
                loadInterstitialAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(AD_LOG_TAG, "Interstitial ad failed to show: ${adError.message} (Code: ${adError.code})")
                mInterstitialAd = null
                loadInterstitialAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(AD_LOG_TAG, "Interstitial ad showed.")
            }
        }
    }

    private fun showInterstitialAdSmart() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastAd = currentTime - lastInterstitialTime

        if (userActionCount >= actionsBeforeInterstitial &&
            timeSinceLastAd >= minInterstitialInterval &&
            mInterstitialAd != null
        ) {
            mInterstitialAd?.show(this)
        } else {
            Log.d(AD_LOG_TAG, "Interstitial ad not ready. Actions: $userActionCount, Time since last ad: ${timeSinceLastAd / 1000}s. Loading status: ${if (isInterstitialLoading) "Loading" else "Not loading"}")
            if (mInterstitialAd == null && !isInterstitialLoading) {
                loadInterstitialAd()
            }
        }
    }

    private fun loadRewardedAd() {
        if (isRewardedLoading || mRewardedAd != null) return

        isRewardedLoading = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            this,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(AD_LOG_TAG, "Rewarded ad failed to load: ${adError.message} (Code: ${adError.code})")
                    mRewardedAd = null
                    isRewardedLoading = false
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    Log.d(AD_LOG_TAG, "Rewarded ad loaded successfully.")
                    mRewardedAd = rewardedAd
                    isRewardedLoading = false
                    setRewardedAdCallbacks()
                }
            }
        )
    }

    private fun setRewardedAdCallbacks() {
        mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(AD_LOG_TAG, "Rewarded ad was dismissed.")
                mRewardedAd = null
                loadRewardedAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(AD_LOG_TAG, "Rewarded ad failed to show: ${adError.message} (Code: ${adError.code})")
                mRewardedAd = null
                loadRewardedAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(AD_LOG_TAG, "Rewarded ad showed.")
            }
        }
    }

    private fun showRewardedAd(onRewardEarned: (Int) -> Unit = {}) {
        if (mRewardedAd != null) {
            mRewardedAd?.show(this) { rewardItem ->
                Log.d(AD_LOG_TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                userViewModel.addCoins(50) // Add 50 coins for watching rewarded ad
                onRewardEarned(rewardItem.amount)
            }
        } else {
            Log.d(AD_LOG_TAG, "Rewarded ad not ready. Loading status: ${if (isRewardedLoading) "Loading" else "Not loading"}")
            if (!isRewardedLoading) {
                loadRewardedAd()
            }
        }
    }

    private fun loadRewardedInterstitialAd() {
        if (isRewardedInterstitialLoading || mRewardedInterstitialAd != null) return

        isRewardedInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()

        RewardedInterstitialAd.load(
            this,
            REWARDED_INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(AD_LOG_TAG, "Rewarded Interstitial ad failed to load: ${adError.message} (Code: ${adError.code})")
                    mRewardedInterstitialAd = null
                    isRewardedInterstitialLoading = false
                }

                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    Log.d(AD_LOG_TAG, "Rewarded Interstitial ad loaded successfully.")
                    mRewardedInterstitialAd = ad
                    isRewardedInterstitialLoading = false
                    setRewardedInterstitialAdCallbacks()
                }
            }
        )
    }

    private fun setRewardedInterstitialAdCallbacks() {
        mRewardedInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(AD_LOG_TAG, "Rewarded Interstitial ad was dismissed.")
                mRewardedInterstitialAd = null
                loadRewardedInterstitialAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(AD_LOG_TAG, "Rewarded Interstitial ad failed to show: ${adError.message} (Code: ${adError.code})")
                mRewardedInterstitialAd = null
                loadRewardedInterstitialAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(AD_LOG_TAG, "Rewarded Interstitial ad showed.")
            }
        }
    }

    private fun showRewardedInterstitialAdForDailyBonus(onRewardEarned: (Boolean) -> Unit) {
        if (mRewardedInterstitialAd != null) {
            mRewardedInterstitialAd?.show(this) { rewardItem ->
                Log.d(AD_LOG_TAG, "User earned reward from Rewarded Interstitial: ${rewardItem.amount} ${rewardItem.type}")
                onRewardEarned(true) // Indicate that reward was earned
            }
        } else {
            Log.d(AD_LOG_TAG, "Rewarded Interstitial ad not ready. Loading status: ${if (isRewardedInterstitialLoading) "Loading" else "Not loading"}")
            if (!isRewardedInterstitialLoading) {
                loadRewardedInterstitialAd()
            }
            onRewardEarned(false) // Indicate that reward was not earned (ad not ready)
        }
    }

    private fun loadNativeAd() {
        if (isNativeAdLoading || mNativeAd != null) return

        isNativeAdLoading = true
        val adLoader = AdLoader.Builder(this, NATIVE_AD_UNIT_ID)
            .forNativeAd { nativeAd ->
                Log.d(AD_LOG_TAG, "Native ad loaded successfully.")
                AdEventLogger.logNativeLoaded()
                mNativeAd = nativeAd
                isNativeAdLoading = false
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(AD_LOG_TAG, "Native ad failed to load: ${adError.message} (Code: ${adError.code})")
                    mNativeAd = null
                    isNativeAdLoading = false
                }

                override fun onAdLoaded() {
                    Log.d(AD_LOG_TAG, "Native ad loaded.")
                }

                override fun onAdClicked() {
                    Log.d(AD_LOG_TAG, "Native ad clicked.")
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    override fun onDestroy() {
        mNativeAd?.destroy()
        super.onDestroy()
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Generate : Screen("generate", "Generate", Icons.Default.Create)
    object Scan : Screen("scan", "Scan", Icons.Default.QrCodeScanner)
    object GainCoins : Screen("gain_coins", "Coins", Icons.Default.MonetizationOn)
    object Redeem : Screen("redeem", "Redeem", Icons.Default.CardGiftcard) // New screen for redeeming codes
    object History : Screen("history", "History", Icons.Default.History) // New screen for history
    object About : Screen("about", "About", Icons.Default.Info)
    object Premium : Screen("premium", "Premium", Icons.Default.Star) // New screen for premium purchase
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
    onShowRewardedInterstitialAdForDailyBonus: (onRewardEarned: (Boolean) -> Unit) -> Unit, // New parameter
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    nativeAd: NativeAd?, // Pass native ad
    isPremiumUser: Boolean // Pass premium status
) {
    val navController = rememberNavController()
    val userUiState by userViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // State to control the visibility of the daily bonus dialog
    var showDailyBonusDialog by remember { mutableStateOf(false) }

    // Listen for daily bonus availability and show dialog
    LaunchedEffect(userUiState.dailyBonusAvailable) {
        if (userUiState.dailyBonusAvailable) {
            showDailyBonusDialog = true
        }
    }

    // Listen for navigation changes to trigger interstitial ads
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect {
            onShowInterstitialAd()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QRWiz", style = MaterialTheme.typography.titleLarge) }, // Smaller title
                navigationIcon = {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    if (currentRoute != Screen.Home.route) { // Show back button on all screens except Home
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer // Consistent tint
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.onSecondary, // Transparent feel
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    // Display coin balance
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp) // Adjusted padding
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coin Balance",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp) // Smaller icon
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = userUiState.coins.toString(),
                            style = MaterialTheme.typography.titleSmall, // Smaller text
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Display diamond balance
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp) // Adjusted padding
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = "Diamond Balance",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp) // Smaller icon
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = userUiState.diamonds.toString(),
                            style = MaterialTheme.typography.titleSmall, // Smaller text
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = Icons.Default.LightMode,
                            contentDescription = "Toggle Dark Mode",
                            tint = MaterialTheme.colorScheme.primary // Consistent tint
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // Consistent Banner Ad at the bottom across all screens
                if (!isPremiumUser) {
                    BannerAd(adUnitId = BANNER_AD_UNIT_ID)
                }
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f) // Transparent feel
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    navItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title, modifier = Modifier.size(24.dp)) }, // Smaller icons
                            label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) }, // Smaller labels
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
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors( // Use specific colors for selected/unselected
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) // Transparent indicator
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
            // No global banner ad here. Each screen will manage its own banner ad.
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
                            diamondBalance = userUiState.diamonds, // Pass diamond balance
                            isPremiumUser = isPremiumUser, // Pass premium status
                            onNavigateToGenerate = { navController.navigate(Screen.Generate.route) },
                            onNavigateToScan = { navController.navigate(Screen.Scan.route) },
                            onNavigateToGainCoins = { navController.navigate(Screen.GainCoins.route) },
                            onNavigateToRedeem = { navController.navigate(Screen.Redeem.route) }, // Navigate to redeem screen
                            onNavigateToHistory = { navController.navigate(Screen.History.route) }, // Navigate to history screen
                            onNavigateToPremium = { navController.navigate(Screen.Premium.route) }, // Navigate to premium screen
                            nativeAd = nativeAd, // Pass native ad
                            showNativeAd = !isPremiumUser // Show native ad if not premium
                        )
                    }
                    composable(Screen.Generate.route) {
                        QRGenScreen(
                            viewModel = qrGenViewModel,
                            coinBalance = userUiState.coins,
                            onDeductCoins = userViewModel::deductCoins,
                            onShowInterstitialAd = onShowInterstitialAd,
                            showToast = { message -> context.showToast(message) },
                            nativeAd = nativeAd, // Pass native ad
                            showNativeAd = !isPremiumUser // Show native ad if not premium
                        )
                    }
                    composable(Screen.Scan.route) {
                        QRScanScreen(
                            viewModel = qrScanViewModel,
                            onAddCoins = { amount ->
                                userViewModel.addCoins(amount)
                            },
                            onAddScanToHistory = userViewModel::addScanToHistory, // Pass history function
                            onShowInterstitialAd = onShowInterstitialAd,
                            isPremiumUser = isPremiumUser, // Pass premium status
                            showToast = { message -> context.showToast(message) },
                            nativeAd = nativeAd, // Pass native ad
                            showNativeAd = false // Native ads removed from QRScan screen
                        )
                    }
                    composable(Screen.GainCoins.route) {
                        GainCoinsScreen(
                            coinBalance = userUiState.coins,
                            onShowRewardedAd = onShowRewardedAd,
                            onNavigateToScan = { navController.navigate(Screen.Scan.route) },
                            onNavigateToPremium = { navController.navigate(Screen.Premium.route) }, // Navigate to premium from here
                            nativeAd = nativeAd, // Pass native ad
                            showNativeAd = !isPremiumUser ,
                            isPremiumUser = isPremiumUser,
                            dailyBonusAvailable = userUiState.dailyBonusAvailable, // Pass daily bonus state
                            dailyBonusAmount = userUiState.dailyBonusAmount, // Pass daily bonus amount
                            dailyStreak = userUiState.dailyStreak, // Pass daily streak
                            dailyBonusPattern = userUiState.dailyBonusPattern, // Pass daily bonus pattern
                            onClaimDailyBonus = {
                                // This lambda will be called from GainCoinsScreen to trigger the ad and claim
                                onShowRewardedInterstitialAdForDailyBonus { adWatchedAndRewarded ->
                                    if (adWatchedAndRewarded) {
                                        userViewModel.claimDailyBonus()
                                        // No need to dismiss dialog here, as it's handled in MainActivity
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
                            nativeAd = nativeAd, // Pass native ad
                            showNativeAd = false // Show native ad if not premium
                        )
                    }
                    composable(Screen.History.route) {
                        HistoryScreen(
                            userViewModel = userViewModel,
                            onNavigateToPremium = { navController.navigate(Screen.Premium.route) }, // Navigate to premium from here
                            showToast = { message -> context.showToast(message) },
                            onShowInterstitialAd = onShowInterstitialAd,
                            nativeAd = nativeAd, // Pass native ad
                            showNativeAd = false // Show native ad if not premium
                        )
                    }
                    composable(Screen.About.route) {
                        AboutScreen(
                            nativeAd = nativeAd, // Pass native ad
                            showNativeAd = !isPremiumUser // Show native ad if not premium
                        )
                    }
                    composable(Screen.Premium.route) {
                        PremiumPlanScreen(
                            userViewModel = userViewModel,
                            showToast = { message -> context.showToast(message) },
                            nativeAd = nativeAd, // Pass native ad
                            showNativeAd = !isPremiumUser // Show native ad if not premium
                        )
                    }
                }
            }
        }
    }

    // Daily Bonus Dialog - Keep this for initial notification on app launch
    if (showDailyBonusDialog && userUiState.dailyBonusAvailable) {
        AlertDialog(
            onDismissRequest = { showDailyBonusDialog = false },
            title = { Text("Daily Login Bonus!") },
            text = { Text("Claim your daily bonus of ${userUiState.dailyBonusAmount} coins! Current streak: ${userUiState.dailyStreak + 1} days.") },
            confirmButton = {
                Button(onClick = {
                    // Attempt to show rewarded interstitial ad
                    onShowRewardedInterstitialAdForDailyBonus { adWatchedAndRewarded ->
                        if (adWatchedAndRewarded) {
                            userViewModel.claimDailyBonus()
                            showDailyBonusDialog = false // Dismiss dialog after successful claim
                        } else {
                            // Ad not watched or not ready, keep dialog open or show a message
                            context.showToast("Ad not ready or not watched. Please try again.")
                        }
                    }
                }) {
                    Text("Claim Now!")
                }
            },
            dismissButton = {
                Button(onClick = { showDailyBonusDialog = false }) {
                    Text("Later")
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
            .height(50.dp) // Standard banner ad height
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)) // Slightly transparent background
            .padding(vertical = 4.dp), // Small padding
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
                        AdEventLogger.logBannerLoaded()
                        Log.d(AD_LOG_TAG, "Banner ad loaded successfully for unit: $adUnitId")
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e(AD_LOG_TAG, "Banner ad failed to load for unit $adUnitId: ${adError.message} (Code: ${adError.code})")
                    }

                    override fun onAdClicked() {
                        Log.d(AD_LOG_TAG, "Banner ad clicked for unit: $adUnitId")
                    }
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
                // This is the correct way to register a click on the native ad
                nativeAd.performClick(Bundle())
                Log.d(AD_LOG_TAG, "Native ad clicked via performClick().")
            }        ,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            // "Sponsored" label
            Text(
                text = "Sponsored",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd) // Align to top end
                    .padding(4.dp) // Small padding
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Display the native ad's media (if available) or a placeholder
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp), // Standard height for native ad media
                    factory = { context ->
                        // This is a simplified representation.
                        // In a real app, you would inflate a NativeAdView layout
                        // and populate it with nativeAd assets (headline, body, media, icon, etc.)
                        // For this example, we'll just show the headline and body.
                        // A more robust implementation would involve a custom layout for the native ad.
                        val adView = AdView(context).apply {
                            setAdSize(AdSize.MEDIUM_RECTANGLE) // Or a suitable size for native ads
                            setAdUnitId(NATIVE_AD_UNIT_ID) // Use the native ad unit ID
                            loadAd(AdRequest.Builder().build()) // Load a test ad
                        }
                        adView
                    },
                    update = { adView ->
                        // Update logic if needed, e.g., reloading ad on configuration change
                        adView.loadAd(AdRequest.Builder().build())
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Display headline
                Text(
                    text = nativeAd.headline ?: "Ad Headline",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Display body
                Text(
                    text = nativeAd.body ?: "Ad Body Text",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        shape = RoundedCornerShape(10.dp), // Slightly less rounded
        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp) // Thinner border
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge) // Smaller text
        if (rewardEarned > 0) {
            Text(" (Rewards: $rewardEarned)", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}
