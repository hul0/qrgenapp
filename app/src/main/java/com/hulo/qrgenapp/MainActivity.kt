package com.hulo.qrgenapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.hulo.qrgenapp.ui.theme.QRGenAppTheme

// IMPORTANT: Use Google's TEST Ad Unit IDs for development and testing.
// Replace these with your actual production IDs ONLY when your app is published.
// Removed BANNER_AD_UNIT_ID_TOP and BANNER_AD_UNIT_ID_BOTTOM as banners will be placed per screen.
private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // Google's Test Interstitial Ad Unit ID
private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917" // Google's Test Rewarded Ad Unit ID
private const val NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110" // Google's Test Native Ad Unit ID
private const val AD_LOG_TAG = "AdMob"

class MainActivity : ComponentActivity() {

    private val qrGenViewModel: QRGenViewModel by viewModels()
    private val qrScanViewModel: QRScanViewModel by viewModels()
    private lateinit var userViewModel: UserViewModel // Initialize UserViewModel later with context

    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd? = null
    private var mNativeAd: NativeAd? = null // New: Native Ad instance

    // Declared userActionCount here
    private var userActionCount = 0
    private var lastInterstitialTime = 0L
    private val minInterstitialInterval = 20000L // 20 seconds minimum between interstitials
    private val actionsBeforeInterstitial = 2 // Show interstitial after 2 user actions (generating/scanning)

    private var isInterstitialLoading = false
    private var isRewardedLoading = false
    private var isNativeAdLoading = false // New: Native ad loading state

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize UserPreferences and UserViewModel
        val userPreferences = UserPreferences(applicationContext)
        userViewModel = UserViewModel(userPreferences)

        MobileAds.initialize(this) { initializationStatus ->
            Log.d(AD_LOG_TAG, "Mobile Ads SDK Initialized: $initializationStatus")
            loadInterstitialAd()
            loadRewardedAd()
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

    private fun loadNativeAd() {
        if (isNativeAdLoading || mNativeAd != null) return

        isNativeAdLoading = true
        val adLoader = AdLoader.Builder(this, NATIVE_AD_UNIT_ID)
            .forNativeAd { nativeAd ->
                Log.d(AD_LOG_TAG, "Native ad loaded successfully.")
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
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    nativeAd: NativeAd?, // Pass native ad
    isPremiumUser: Boolean // Pass premium status
) {
    val navController = rememberNavController()
    val userUiState by userViewModel.uiState.collectAsState()
    val context = LocalContext.current

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
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f), // Transparent feel
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp) // Smaller icon
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = userUiState.coins.toString(),
                            style = MaterialTheme.typography.titleSmall, // Smaller text
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
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
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp) // Smaller icon
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = userUiState.diamonds.toString(),
                            style = MaterialTheme.typography.titleSmall, // Smaller text
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = Icons.Default.SettingsBrightness,
                            contentDescription = "Toggle Dark Mode",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer // Consistent tint
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // Banner Ad will now be handled within each screen composable
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
                            showNativeAd = !isPremiumUser // Show native ad if not premium
                        )
                    }
                    composable(Screen.GainCoins.route) {
                        GainCoinsScreen(
                            coinBalance = userUiState.coins,
                            onShowRewardedAd = onShowRewardedAd,
                            onNavigateToPremium = { navController.navigate(Screen.Premium.route) }, // Navigate to premium from here
                            nativeAd = nativeAd, // Pass native ad
                            showNativeAd = !isPremiumUser // Show native ad if not premium
                        )
                    }
                    composable(Screen.Redeem.route) {
                        RedeemCodeScreen(
                            userViewModel = userViewModel,
                            showToast = { message -> context.showToast(message) },
                            nativeAd = nativeAd, // Pass native ad
                            showNativeAd = !isPremiumUser // Show native ad if not premium
                        )
                    }
                    composable(Screen.History.route) {
                        HistoryScreen(
                            userViewModel = userViewModel,
                            onNavigateToPremium = { navController.navigate(Screen.Premium.route) }, // Navigate to premium from here
                            showToast = { message -> context.showToast(message) },
                            onShowInterstitialAd = onShowInterstitialAd,
                            nativeAd = nativeAd, // Pass native ad
                            showNativeAd = !isPremiumUser // Show native ad if not premium
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
        Spacer(modifier = Modifier.height(0.dp)) // Return empty space if ad not shown or null
        return
    }

    // This is a placeholder for a Native Ad.
    // Integrating actual NativeAdView in Compose requires an AndroidView and setting up the view hierarchy
    // to display the ad assets (headline, body, image, etc.).
    // For simplicity, this composable just shows a card with a placeholder text.
    // You would replace this with your actual NativeAdView implementation.
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp) // Adjusted height for a smaller native ad
            .padding(horizontal = 12.dp, vertical = 6.dp), // Smaller padding
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Smaller elevation
        shape = RoundedCornerShape(12.dp), // Slightly less rounded
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f)) // Transparent feel
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Star, // Placeholder icon
                    contentDescription = "Ad",
                    modifier = Modifier.size(36.dp), // Smaller icon
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp)) // Smaller spacer
                Text(
                    text = "Native Ad Placeholder",
                    style = MaterialTheme.typography.titleSmall, // Smaller text
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = nativeAd.headline ?: "Ad Loading...", // Example of using native ad data
                    style = MaterialTheme.typography.bodySmall, // Smaller text
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
