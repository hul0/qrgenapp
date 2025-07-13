package com.hulo.qrgenapp

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.nativead.NativeAd

@Composable
fun GainCoinsScreen(
    coinBalance: Int,
    onNavigateToScan: () -> Unit,
    onShowRewardedAd: (onRewardEarned: (Int) -> Unit) -> Unit,
    onNavigateToPremium: () -> Unit,
    nativeAd: NativeAd?,
    showNativeAd: Boolean,
    isPremiumUser: Boolean,
    dailyBonusAvailable: Boolean,
    dailyBonusAmount: Int,
    dailyStreak: Int,
    dailyBonusPattern: List<Int>,
    onClaimDailyBonus: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Icon(
            imageVector = Icons.Default.MonetizationOn,
            contentDescription = "Coins",
            modifier = Modifier.size(80.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Earn More Coins!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Coins allow you to generate QR codes. Here's how you can get more:",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Daily Login Bonus Card
        if (dailyBonusAvailable) {
            CoinEarningMethodCard(
                title = "Daily Login Bonus!",
                description = "Claim your $dailyBonusAmount coins for logging in today! Current streak: ${dailyStreak + 1} days.",
                icon = Icons.Default.Star,
                buttonText = "Claim Daily Bonus",
                onClick = onClaimDailyBonus
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Upcoming Daily Rewards Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                    color = Color.Black.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
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
                                modifier = Modifier.size(28.dp),
                                tint = if (index == dailyStreak && dailyBonusAvailable) Color(0xFF8B5CF6) else Color.Gray.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "+$amount",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (index == dailyStreak && dailyBonusAvailable) Color(0xFF8B5CF6) else Color.Gray.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Day ${index + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Ways to earn coins
        CoinEarningMethodCard(
            title = "Watch Rewarded Ads",
            description = "Earn +50 coins for each rewarded ad you watch.",
            icon = Icons.Default.VideoStable,
            buttonText = "Watch Ad",
            onClick = { onShowRewardedAd { /* reward handled in MainActivity */ } }
        )
        Spacer(modifier = Modifier.height(16.dp))
        CoinEarningMethodCard(
            title = "Scan QR Codes",
            description = "Get +5 coins every time you successfully scan a QR code.",
            icon = Icons.Default.QrCodeScanner,
            buttonText = "Go to Scanner",
            onClick = onNavigateToScan
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Current Balance
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = "Current Balance",
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF8B5CF6)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your Balance: $coinBalance Coins",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(alpha = 0.8f)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Native Ad Section
        NativeAdViewComposable(
            nativeAd = nativeAd,
            showAd = showNativeAd
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Premium Plan Section
        if (!isPremiumUser) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Premium Plan",
                        modifier = Modifier.size(56.dp),
                        tint = Color(0xFF6366F1)
                    )
                    Text(
                        text = "Unlock Premium Features!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Go ad-free, get unlimited history, and more!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onNavigateToPremium,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B5CF6)
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.Diamond,
                            contentDescription = "Buy Premium",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Buy Premium with Diamonds")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
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
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Color(0xFF6366F1)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(buttonText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
