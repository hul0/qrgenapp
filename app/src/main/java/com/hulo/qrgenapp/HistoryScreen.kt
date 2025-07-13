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
    showAd: Boolean // New: Control native ad visibility (renamed from showNativeAd for consistency)
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
            .padding(horizontal = 16.dp), // Apply overall horizontal padding
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Native Ad Section (moved to top for consistent placement with other screens)
        if (showAd) {
            Spacer(modifier = Modifier.height(16.dp)) // Spacer before ad
            NativeAdViewComposable(
                nativeAd = nativeAd,
                showAd = showAd,
                modifier = Modifier.fillMaxWidth() // Ensure it fills width
            )
            Spacer(modifier = Modifier.height(16.dp)) // Spacer after ad
        } else {
            Spacer(modifier = Modifier.height(32.dp)) // Larger top spacer if no ad
        }

        Icon(
            imageVector = Icons.Default.History,
            contentDescription = "Scan History",
            modifier = Modifier.size(96.dp), // Larger icon
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp)) // Increased spacer
        Text(
            text = "Scan History",
            style = MaterialTheme.typography.headlineMedium, // Larger headline
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (!isPremium) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp), // Increased vertical padding for card
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // Increased elevation
                shape = RoundedCornerShape(16.dp), // More rounded
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)) // Less transparent, more solid
            ) {
                Column(
                    modifier = Modifier.padding(20.dp), // Increased padding inside card
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Increased spacing
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Premium Feature",
                        modifier = Modifier.size(36.dp), // Larger icon
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Unlock Unlimited History with Premium!",
                        style = MaterialTheme.typography.titleLarge, // Larger title
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Free users can see up to ${UserPreferences.FREE_USER_HISTORY_LIMIT} recent scans. Go Premium for unlimited history and more!",
                        style = MaterialTheme.typography.bodyLarge, // Larger body text
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp)) // Increased spacer
                    Button(
                        onClick = onNavigateToPremium,
                        shape = RoundedCornerShape(12.dp), // More rounded button
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), // Solid primary color
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp) // Increased padding
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Premium", modifier = Modifier.size(20.dp)) // Larger icon
                        Spacer(Modifier.width(8.dp)) // Increased spacing
                        Text("Go Premium!", style = MaterialTheme.typography.titleMedium) // Larger text style
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp)) // Increased spacer
        }

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
                    modifier = Modifier.size(80.dp), // Larger icon
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) // Less transparent
                )
                Spacer(modifier = Modifier.height(16.dp)) // Increased spacer
                Text(
                    text = "No scan history yet. Start scanning QR codes!",
                    style = MaterialTheme.typography.titleLarge, // Larger text
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
                verticalArrangement = Arrangement.spacedBy(8.dp), // Increased spacing between items
                contentPadding = PaddingValues(vertical = 8.dp) // Increased vertical padding
            ) {
                items(scanHistory) { scanResult ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Increased elevation
                        shape = RoundedCornerShape(12.dp), // More rounded
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.95f)) // Less transparent, more solid
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp), // Increased padding inside item card
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween // Distribute content
                        ) {
                            Text(
                                text = scanResult,
                                style = MaterialTheme.typography.bodyLarge, // Larger text
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f) // Allow text to take available space
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(scanResult))
                                    context.showToast("Copied to clipboard!")
                                    if (!isPremium) { // Only show ad if not premium
                                        onShowInterstitialAd() // Show interstitial ad after copy
                                    }
                                },
                                modifier = Modifier.size(48.dp) // Larger touch target for icon button
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy to Clipboard",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp) // Larger icon
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp)) // Increased spacer
            Button(
                onClick = {
                    userViewModel.clearScanHistory()
                    showToast("History cleared!")
                },
                shape = RoundedCornerShape(12.dp), // More rounded button
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), // Solid error color
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp) // Increased padding
            ) {
                Text("Clear History", style = MaterialTheme.typography.titleMedium) // Larger text style
            }
        }
        Spacer(modifier = Modifier.height(24.dp)) // Increased bottom spacer
    }
}
