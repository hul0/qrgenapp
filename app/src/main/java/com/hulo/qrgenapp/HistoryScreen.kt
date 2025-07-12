package com.hulo.qrgenapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy // Import for copy icon
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager // Import for clipboard
import androidx.compose.ui.platform.LocalContext // Import for toast
import androidx.compose.ui.text.AnnotatedString // Import for clipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.nativead.NativeAd

// Ad Unit ID for banner ad on this screen
private const val BANNER_AD_UNIT_ID_HISTORY_SCREEN = "ca-app-pub-3940256099942544/6300978111" // Google's Test Banner Ad Unit ID

@Composable
fun HistoryScreen(
    userViewModel: UserViewModel,
    onNavigateToPremium: () -> Unit,
    showToast: (String) -> Unit,
    onShowInterstitialAd: () -> Unit, // New: Callback for showing interstitial ads
    nativeAd: NativeAd?, // New: Native Ad
    showNativeAd: Boolean // New: Control native ad visibility
) {
    val uiState by userViewModel.uiState.collectAsState()
    val scanHistory = uiState.scanHistory
    val isPremium = uiState.isPremium
    val clipboardManager = LocalClipboardManager.current // Get clipboard manager
    val context = LocalContext.current // Get context for toast

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Ensure background matches theme
            .verticalScroll(rememberScrollState()) // Ensure the entire screen is scrollable
            .padding(horizontal = 12.dp), // Apply overall smaller horizontal padding
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Banner Ad at the top of the screen
        if (showNativeAd) { // showNativeAd is true if not premium
            BannerAd(
                adUnitId = BANNER_AD_UNIT_ID_HISTORY_SCREEN,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp)) // Small spacer after ad
        }

        Spacer(modifier = Modifier.height(12.dp)) // Smaller top spacer

        Icon(
            imageVector = Icons.Default.History,
            contentDescription = "Scan History",
            modifier = Modifier.size(24.dp), // Slightly larger icon for title
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp)) // Smaller spacer
        Text(
            text = "Scan History",
            style = MaterialTheme.typography.headlineSmall, // Smaller headline
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (!isPremium) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp), // Reduced vertical padding for card
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Reduced elevation
                shape = RoundedCornerShape(12.dp), // Slightly less rounded
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)) // Transparent feel
            ) {
                Column(
                    modifier = Modifier.padding(12.dp), // Reduced padding inside card
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp) // Reduced spacing
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Premium Feature",
                        modifier = Modifier.size(20.dp), // Smaller icon
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Unlock Unlimited History with Premium!",
                        style = MaterialTheme.typography.titleSmall, // Smaller title
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Free users can see up to ${UserPreferences.FREE_USER_HISTORY_LIMIT} recent scans. Go Premium for unlimited history and more!",
                        style = MaterialTheme.typography.bodySmall, // Smaller body text
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp)) // Reduced spacer
                    Button(
                        onClick = onNavigateToPremium,
                        shape = RoundedCornerShape(10.dp), // Slightly less rounded button
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)), // Transparent feel
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp) // Smaller padding
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Premium", modifier = Modifier.size(16.dp)) // Smaller icon
                        Spacer(Modifier.width(6.dp)) // Reduced spacing
                        Text("Go Premium!", style = MaterialTheme.typography.labelLarge) // Smaller text style
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp)) // Reduced spacer
        }

        // Native Ad Section
        NativeAdViewComposable(
            nativeAd = nativeAd,
            showAd = showNativeAd
        )
        Spacer(modifier = Modifier.height(12.dp)) // Small spacer after ad


        if (scanHistory.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Take available space
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "No History",
                    modifier = Modifier.size(56.dp), // Smaller icon
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) // More transparent
                )
                Spacer(modifier = Modifier.height(12.dp)) // Smaller spacer
                Text(
                    text = "No scan history yet. Start scanning QR codes!",
                    style = MaterialTheme.typography.titleSmall, // Smaller text
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // LazyColumn for efficient scrolling of history items - now takes more weight
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(3f), // Increased weight to make it the biggest field
                verticalArrangement = Arrangement.spacedBy(6.dp), // Reduced spacing between items
                contentPadding = PaddingValues(vertical = 4.dp) // Reduced vertical padding
            ) {
                items(scanHistory) { scanResult ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), // Slightly less elevation
                        shape = RoundedCornerShape(10.dp), // Slightly less rounded
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f)) // Transparent feel
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp), // Reduced padding inside item card
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween // Distribute content
                        ) {
                            Text(
                                text = scanResult,
                                style = MaterialTheme.typography.bodySmall, // Slightly smaller text
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f) // Allow text to take available space
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(scanResult))
                                    context.showToast("Copied to clipboard!")
                                    if (!isPremium) { // Only show ad if not premium
                                        onShowInterstitialAd() // Show interstitial ad after copy
                                    }
                                },
                                modifier = Modifier.size(36.dp) // Smaller touch target for icon button
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy to Clipboard",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp) // Smaller icon
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp)) // Reduced spacer
            Button(
                onClick = {
                    userViewModel.clearScanHistory()
                    showToast("History cleared!")
                },
                shape = RoundedCornerShape(10.dp), // Slightly less rounded button
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)), // Transparent feel
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp) // Smaller padding
            ) {
                Text("Clear History", style = MaterialTheme.typography.labelLarge) // Smaller text style
            }
        }
        Spacer(modifier = Modifier.height(12.dp)) // Slightly smaller bottom spacer
    }
}
