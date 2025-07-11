package com.hulo.qrgenapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.hulo.qrgenapp.ui.theme.QRGenAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// IMPORTANT: Use Google's TEST Ad Unit IDs for development and testing.
// Replace these with your actual production IDs ONLY when your app is published.
private const val BANNER_AD_UNIT_ID_TOP = "ca-app-pub-3940256099942544/6300978111" // Test Banner
private const val BANNER_AD_UNIT_ID_BOTTOM = "ca-app-pub-3940256099942544/6300978111" // Test Banner
private const val BANNER_AD_UNIT_ID_INLINE = "ca-app-pub-3940256099942544/6300978111" // Test Banner
private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // Test Interstitial
private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917" // Test Rewarded
private const val AD_LOG_TAG = "AdMob"

class MainActivity : ComponentActivity() {

    private val qrGenViewModel: QRGenViewModel by viewModels()
    private val qrScanViewModel: QRScanViewModel by viewModels()
    private lateinit var userViewModel: UserViewModel // Initialize UserViewModel later with context

    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd? = null

    // Declared userActionCount here
    private var userActionCount = 0
    private var lastInterstitialTime = 0L
    private val minInterstitialInterval = 5000L // 5 seconds minimum between interstitials
    private val actionsBeforeInterstitial = 1 // Show interstitial after 1 user action (generating/scanning)

    private var isInterstitialLoading = false
    private var isRewardedLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize UserPreferences and UserViewModel
        val userPreferences = UserPreferences(applicationContext)
        userViewModel = UserViewModel(userPreferences)

        MobileAds.initialize(this) { initializationStatus ->
            Log.d(AD_LOG_TAG, "Mobile Ads SDK Initialized: $initializationStatus")
            loadInterstitialAd()
            loadRewardedAd()
        }

        setContent {
            var darkTheme by remember { mutableStateOf(false) } // State for dark mode toggle

            QRGenAppTheme(darkTheme = darkTheme) { // Apply the custom theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppScreen(
                        qrGenViewModel = qrGenViewModel,
                        qrScanViewModel = qrScanViewModel,
                        userViewModel = userViewModel, // Pass UserViewModel
                        onShowInterstitialAd = ::showInterstitialAdSmart,
                        onShowRewardedAd = ::showRewardedAd,
                        darkTheme = darkTheme,
                        onToggleTheme = { darkTheme = !darkTheme } // Pass toggle function
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
                userActionCount = 0
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
            mInterstitialAd != null) {

            mInterstitialAd?.show(this)
            userActionCount = 0 // Reset after showing
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
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Generate : Screen("generate", "Generate", Icons.Default.Create)
    object Scan : Screen("scan", "Scan", Icons.Default.QrCodeScanner)
    object GainCoins : Screen("gain_coins", "Coins", Icons.Default.MonetizationOn) // New screen for gaining coins
    object About : Screen("about", "About", Icons.Default.Info)
}

val navItems = listOf(Screen.Home, Screen.Generate, Screen.Scan, Screen.GainCoins, Screen.About)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    qrGenViewModel: QRGenViewModel,
    qrScanViewModel: QRScanViewModel,
    userViewModel: UserViewModel, // UserViewModel parameter
    onShowInterstitialAd: () -> Unit,
    onShowRewardedAd: (onRewardEarned: (Int) -> Unit) -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val navController = rememberNavController()
    var showInlineAd by remember { mutableStateOf(false) }
    val userUiState by userViewModel.uiState.collectAsState() // Collect user UI state
    val context = LocalContext.current // Get context to show toasts

    LaunchedEffect(Unit) {
        delay(10000)
        showInlineAd = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Code Pro") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Display coin balance in the top bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coin Balance",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = userUiState.coins.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = Icons.Default.SettingsBrightness,
                            contentDescription = "Toggle Dark Mode"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column {
                BannerAd(
                    adUnitId = BANNER_AD_UNIT_ID_BOTTOM,
                    modifier = Modifier.fillMaxWidth()
                )

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    navItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
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
            BannerAd(
                adUnitId = BANNER_AD_UNIT_ID_TOP,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route, // Set Home as the start destination
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            coinBalance = userUiState.coins, // Pass coin balance
                            onNavigateToGenerate = { navController.navigate(Screen.Generate.route) },
                            onNavigateToScan = { navController.navigate(Screen.Scan.route) },
                            onNavigateToGainCoins = { navController.navigate(Screen.GainCoins.route) } // Navigate to gain coins screen
                        )
                    }
                    composable(Screen.Generate.route) {
                        QRGenScreen(
                            viewModel = qrGenViewModel,
                            coinBalance = userUiState.coins, // Pass coin balance
                            onDeductCoins = userViewModel::deductCoins, // Pass deduct coins function
                            onShowInterstitialAd = onShowInterstitialAd,
                            // Removed onShowRewardedAd from here as per request
                            showInlineAd = showInlineAd,
                            showToast = { message -> context.showToast(message) } // Pass Android Context's showToast
                        )
                    }
                    composable(Screen.Scan.route) {
                        QRScanScreen(
                            viewModel = qrScanViewModel,
                            onAddCoins = { amount ->
                                userViewModel.addCoins(amount)
                                // userActionCount increment for interstitial is handled by onShowInterstitialAd call inside QRScanScreen
                            },
                            onShowInterstitialAd = onShowInterstitialAd,
                            // Removed onShowRewardedAd from here as per request
                            showInlineAd = showInlineAd,

                        )
                    }
                    composable(Screen.GainCoins.route) {
                        GainCoinsScreen(
                            coinBalance = userUiState.coins,
                            onShowRewardedAd = onShowRewardedAd // Rewarded ad only shown here
                        )
                    }
                    composable(Screen.About.route) {
                        AboutScreen()
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
        modifier = modifier,
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
fun InlineBannerAd(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    if (isVisible) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            BannerAd(
                adUnitId = BANNER_AD_UNIT_ID_INLINE,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            )
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
        shape = RoundedCornerShape(12.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp)
    ) {
        Text(text)
        if (rewardEarned > 0) {
            Text(" (Rewards: $rewardEarned)", color = MaterialTheme.colorScheme.primary)
        }
    }
}
