package com.hulo.qrgenapp

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoStable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.nativead.NativeAd

// Ad Unit ID for banner ad on this screen
private const val BANNER_AD_UNIT_ID_GAIN_COINS_SCREEN = "ca-app-pub-3940256099942544/6300978111" // Google's Test Banner Ad Unit ID

@Composable
fun GainCoinsScreen(
    coinBalance: Int,
    onNavigateToScan: () -> Unit,
    onShowRewardedAd: (onRewardEarned: (Int) -> Unit) -> Unit,
    onNavigateToPremium: () -> Unit, // New: Navigate to premium screen
    nativeAd: NativeAd?, // New: Native Ad
    showNativeAd: Boolean,
    isPremiumUser: Boolean,
    dailyBonusAvailable: Boolean, // New: Daily bonus available state
    dailyBonusAmount: Int, // New: Daily bonus amount
    dailyStreak: Int, // New: Daily streak (0-6)
    dailyBonusPattern: List<Int>, // New: Daily bonus pattern for upcoming rewards
    onClaimDailyBonus: () -> Unit // New: Callback to claim daily bonus
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp), // Overall smaller horizontal padding
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Banner Ad at the top of the screen (if you want one here, uncomment and add)
        // if (showNativeAd) {
        //     BannerAd(adUnitId = BANNER_AD_UNIT_ID_GAIN_COINS_SCREEN)
        //     Spacer(modifier = Modifier.height(8.dp))
        // }

        Spacer(modifier = Modifier.height(16.dp)) // Smaller top spacer

        Icon(
            imageVector = Icons.Default.MonetizationOn,
            contentDescription = "Coins",
            modifier = Modifier.size(80.dp), // Smaller icon
            tint = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.height(12.dp)) // Smaller spacer
        Text(
            text = "Earn More Coins!",
            style = MaterialTheme.typography.headlineSmall, // Smaller headline
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp)) // Smaller spacer
        Text(
            text = "Coins allow you to generate QR codes. Here's how you can get more:",
            style = MaterialTheme.typography.bodyMedium, // Smaller text
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp) // Smaller horizontal padding
        )
        Spacer(modifier = Modifier.height(24.dp)) // Smaller spacer

        // Daily Login Bonus Card (New)
        if (dailyBonusAvailable) {
            CoinEarningMethodCard(
                title = "Daily Login Bonus!",
                description = "Claim your ${dailyBonusAmount} coins for logging in today! Current streak: ${dailyStreak + 1} days.",
                icon = Icons.Default.Star, // Using Star icon for daily bonus
                buttonText = "Claim Daily Bonus",
                onClick = onClaimDailyBonus // This will trigger the ad and claim in MainActivity
            )
            Spacer(modifier = Modifier.height(12.dp)) // Smaller spacer
        }

        // Upcoming Daily Rewards Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Upcoming Daily Rewards",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dailyBonusPattern.forEachIndexed { index, amount ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (index == dailyStreak && dailyBonusAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "+$amount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (index == dailyStreak && dailyBonusAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Day ${index + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp)) // Smaller spacer


        // Ways to earn coins with updated styling
        CoinEarningMethodCard(
            title = "Watch Rewarded Ads",
            description = "Earn +50 coins for each rewarded ad you watch.",
            icon = Icons.Default.VideoStable,
            buttonText = "Watch Ad",
            onClick = { onShowRewardedAd { /* reward handled in MainActivity */ } }
        )
        Spacer(modifier = Modifier.height(12.dp)) // Smaller spacer
        CoinEarningMethodCard(
            title = "Scan QR Codes",
            description = "Get +5 coins every time you successfully scan a QR code (requires internet).",
            icon = Icons.Default.QrCodeScanner,
            buttonText = "Go to Scanner",
            onClick = onNavigateToScan
        )
        Spacer(modifier = Modifier.height(24.dp)) // Smaller spacer

        // Current Balance with enhanced visibility
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp), // Smaller horizontal padding
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)), // Transparent feel
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), // Smaller padding
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = "Current Balance",
                    modifier = Modifier.size(28.dp), // Smaller icon
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(6.dp)) // Smaller spacer
                Text(
                    text = "Your Current Balance: $coinBalance Coins",
                    style = MaterialTheme.typography.titleMedium, // Smaller text
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp)) // Smaller spacer

        // Native Ad Section
        NativeAdViewComposable(
            nativeAd = nativeAd,
            showAd = showNativeAd
        )
        Spacer(modifier = Modifier.height(16.dp)) // Small spacer after ad

        // Premium Plan Section (moved from QRScan's PremiumFeaturesOverlay)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp), // Smaller horizontal padding
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)), // Transparent feel
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {

            if(!isPremiumUser){Column(
                modifier = Modifier.padding(16.dp), // Smaller padding
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp) // Smaller spacing
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Premium Plan",
                    modifier = Modifier.size(56.dp), // Smaller icon
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Unlock Premium Features!",
                    style = MaterialTheme.typography.titleLarge, // Smaller headline
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Go ad-free, get unlimited history, and more!",
                    style = MaterialTheme.typography.bodyMedium, // Smaller text
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp)) // Smaller spacer
                Button(
                    onClick = onNavigateToPremium,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.9f
                        )
                    ), // Transparent feel
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ) // Smaller padding
                ) {
                    Icon(
                        Icons.Default.Diamond,
                        contentDescription = "Buy Premium",
                        modifier = Modifier.size(18.dp)
                    ) // Smaller icon
                    Spacer(Modifier.width(6.dp)) // Smaller spacer
                    Text(
                        "Buy Premium with Diamonds",
                        style = MaterialTheme.typography.labelLarge
                    ) // Smaller text
                }
            }
            }
        }
        Spacer(modifier = Modifier.height(16.dp)) // Smaller bottom spacer
    }
}

@Composable
fun CoinEarningMethodCard(
    title: String,
    description: String,
    icon: ImageVector,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp) // Smaller horizontal padding
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)), // Animation for size changes
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp), // More rounded corners
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)) // Transparent feel
    ) {
        Column(
            modifier = Modifier.padding(16.dp), // Smaller padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp), // Smaller icon
                tint = MaterialTheme.colorScheme.secondary // Secondary color for icons
            )
            Spacer(modifier = Modifier.height(10.dp)) // Smaller spacer
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium, // Smaller title
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp)) // Smaller spacer
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall, // Smaller text
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)), // Transparent feel
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp) // Smaller padding
            ) {
                Text(buttonText, style = MaterialTheme.typography.labelLarge) // Smaller text
            }
        }
    }
}
