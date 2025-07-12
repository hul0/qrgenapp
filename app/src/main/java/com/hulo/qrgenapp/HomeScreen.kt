package com.hulo.qrgenapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.nativead.NativeAd

// Ad Unit ID for banner ad on this screen
private const val BANNER_AD_UNIT_ID_HOME_SCREEN = "ca-app-pub-3940256099942544/6300978111" // Google's Test Banner Ad Unit ID

@Composable
fun HomeScreen(
    coinBalance: Int,
    diamondBalance: Int,
    isPremiumUser: Boolean,
    onNavigateToGenerate: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToGainCoins: () -> Unit,
    onNavigateToRedeem: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPremium: () -> Unit,
    nativeAd: NativeAd?, // Native Ad
    showNativeAd: Boolean // Control native ad visibility
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Ensure background matches theme
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp), // Overall smaller horizontal padding
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Banner Ad at the top of the screen
        if (showNativeAd) { // showNativeAd is true if not premium
            BannerAd(
                adUnitId = BANNER_AD_UNIT_ID_HOME_SCREEN,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp)) // Small spacer after ad
        }

        Spacer(modifier = Modifier.height(16.dp)) // Smaller top spacer

        // Balance Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp), // Smaller horizontal padding
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)), // Transparent feel
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), // Smaller padding
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp) // Smaller spacing
            ) {
                Text(
                    text = "Your Balances",
                    style = MaterialTheme.typography.titleMedium, // Smaller title
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BalanceItem(
                        icon = Icons.Default.MonetizationOn,
                        label = "Coins",
                        value = coinBalance.toString(),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    BalanceItem(
                        icon = Icons.Default.Diamond,
                        label = "Diamonds",
                        value = diamondBalance.toString(),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // Smaller spacer

        // Quick Actions Grid
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleLarge, // Smaller title
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, bottom = 12.dp) // Adjusted padding
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp), // Smaller horizontal padding
            verticalArrangement = Arrangement.spacedBy(10.dp) // Smaller spacing between rows
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                QuickActionButton(
                    icon = Icons.Default.QrCodeScanner,
                    label = "Scan QR",
                    onClick = onNavigateToScan,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                QuickActionButton(
                    icon = Icons.Default.Create,
                    label = "Generate QR",
                    onClick = onNavigateToGenerate,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                QuickActionButton(
                    icon = Icons.Default.MonetizationOn,
                    label = "Earn Coins",
                    onClick = onNavigateToGainCoins,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                QuickActionButton(
                    icon = Icons.Default.CardGiftcard,
                    label = "Redeem Code",
                    onClick = onNavigateToRedeem,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                QuickActionButton(
                    icon = Icons.Default.History,
                    label = "History",
                    onClick = onNavigateToHistory,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                QuickActionButton(
                    icon = Icons.Default.Star,
                    label = "Premium",
                    onClick = onNavigateToPremium,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // Smaller spacer

        // Native Ad Section
        NativeAdViewComposable(
            nativeAd = nativeAd,
            showAd = showNativeAd
        )
        Spacer(modifier = Modifier.height(16.dp)) // Small spacer after ad
    }
}

@Composable
fun BalanceItem(icon: ImageVector, label: String, value: String, tint: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(28.dp), // Smaller icon
            tint = tint
        )
        Spacer(modifier = Modifier.height(4.dp)) // Smaller spacer
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium, // Smaller label
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge, // Smaller value
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp) // Smaller card height
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)), // Transparent feel
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Smaller elevation
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp) // Smaller padding
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(36.dp), // Smaller icon
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(6.dp)) // Smaller spacer
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge, // Smaller text
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
