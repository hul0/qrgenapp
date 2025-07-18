package com.hulo.qrgenapp

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.nativead.NativeAd

// A sealed interface to better manage the different types of content in our list.
sealed interface GainCoinsScreenItem {
    data class Header(val coinBalance: Int) : GainCoinsScreenItem
    data class DailyBonus(
        val isAvailable: Boolean,
        val amount: Int,
        val streak: Int,
        val onClaim: () -> Unit
    ) : GainCoinsScreenItem
    data class DailyStreakView(
        val pattern: List<Int>,
        val currentStreak: Int,
        val isBonusAvailable: Boolean
    ) : GainCoinsScreenItem
    data class EarningMethod(
        val title: String,
        val description: String,
        val icon: ImageVector,
        val buttonText: String,
        val onClick: () -> Unit
    ) : GainCoinsScreenItem
    data class Ad(val nativeAd: NativeAd?) : GainCoinsScreenItem
    object PremiumPromo : GainCoinsScreenItem
}


/**
 * A custom shimmer animation modifier.
 * This creates a beautiful loading or highlighting effect.
 */
fun Modifier.shimmerBackground(shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1500, easing = FastOutLinearInEasing),
            RepeatMode.Restart
        ), label = "shimmer_translate"
    )
    val shimmerColors = listOf(
        Color(0xFFB8B5B5).copy(alpha = 0.6f),
        Color(0xFFB8B5B5).copy(alpha = 0.2f),
        Color(0xFFB8B5B5).copy(alpha = 0.6f)
    )
    background(
        brush = Brush.horizontalGradient(
            colors = shimmerColors,
            startX = translateAnimation - 200f,
            endX = translateAnimation
        ),
        shape = shape
    )
}

/**
 * The main, polished screen for gaining coins.
 */
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
    // We build a list of items to display in the LazyColumn.
    // This makes the layout logic clean and easy to manage.
    val screenItems = buildList {
        add(GainCoinsScreenItem.Header(coinBalance))
        if (dailyBonusAvailable) {
            add(GainCoinsScreenItem.DailyBonus(
                isAvailable = true,
                amount = dailyBonusAmount,
                streak = dailyStreak,
                onClaim = onClaimDailyBonus
            ))
        }
        add(GainCoinsScreenItem.DailyStreakView(dailyBonusPattern, dailyStreak, dailyBonusAvailable))
        add(GainCoinsScreenItem.EarningMethod(
            title = "Watch an Ad",
            description = "Get +50 coins for each video you watch.",
            icon = Icons.Default.Videocam,
            buttonText = "Watch Now",
            onClick = { onShowRewardedAd { /* handled in MainActivity */ } }
        ))
        add(GainCoinsScreenItem.EarningMethod(
            title = "Scan QR Codes",
            description = "Earn +5 coins for every successful scan.",
            icon = Icons.Default.QrCodeScanner,
            buttonText = "Start Scanning",
            onClick = onNavigateToScan
        ))
        if (showNativeAd) {
            add(GainCoinsScreenItem.Ad(nativeAd))
        }
        if (!isPremiumUser) {
            add(GainCoinsScreenItem.PremiumPromo)
        }
    }

    // The main container with a beautiful gradient background.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2E3192), Color(0xFF1B1464))
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(screenItems.size) { index ->
                val item = screenItems[index]
                // We add spacing between items, but not before the header.
                if (index > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // Render the correct composable based on the item type.
                when (item) {
                    is GainCoinsScreenItem.Header -> ScreenHeader(item.coinBalance)
                    is GainCoinsScreenItem.DailyBonus -> DailyBonusCard(item.amount, item.streak, item.onClaim)
                    is GainCoinsScreenItem.DailyStreakView -> DailyStreakProgressView(item.pattern, item.currentStreak, item.isBonusAvailable)
                    is GainCoinsScreenItem.EarningMethod -> EarningMethodCard(item.title, item.description, item.icon, item.buttonText, item.onClick)
                    is GainCoinsScreenItem.Ad -> NativeAdViewComposable(nativeAd = item.nativeAd, showAd = true)
                    is GainCoinsScreenItem.PremiumPromo -> PremiumPromoCard(onNavigateToPremium)
                }
            }
        }
    }
}

@Composable
fun ScreenHeader(coinBalance: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MonetizationOn,
            contentDescription = "Earn Coins",
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(60.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Coin Center",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Your current balance: $coinBalance coins",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun DailyBonusCard(amount: Int, streak: Int, onClaim: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClaim),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFFC371), Color(0xFFFF5F6D))
                    )
                )
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "DAILY BONUS AVAILABLE!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Claim +$amount coins now!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = "Your streak: ${streak + 1} days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            // Add a subtle shimmer to the claimable card to attract attention
            Box(modifier = Modifier.matchParentSize().shimmerBackground(RoundedCornerShape(20.dp)))
        }
    }
}

@Composable
fun DailyStreakProgressView(pattern: List<Int>, currentStreak: Int, isBonusAvailable: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("Your 7-Day Streak", fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            pattern.forEachIndexed { index, amount ->
                val isClaimed = index < currentStreak
                val isClaimableToday = index == currentStreak && isBonusAvailable
                DailyRewardItem(day = index + 1, amount = amount, isClaimed = isClaimed, isClaimableToday = isClaimableToday)
            }
        }
    }
}

@Composable
fun DailyRewardItem(day: Int, amount: Int, isClaimed: Boolean, isClaimableToday: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "wobble")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isClaimableToday) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "wobble_scale"
    )

    val color = when {
        isClaimed -> Color(0xFFFFD700)
        isClaimableToday -> Color(0xFF4CAF50)
        else -> Color.Gray.copy(alpha = 0.5f)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isClaimed) {
                Icon(Icons.Default.Check, contentDescription = "Claimed", tint = Color.Black)
            } else {
                Text("+$amount", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
            }
        }
        Text("Day $day", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}


@Composable
fun EarningMethodCard(
    title: String,
    description: String,
    icon: ImageVector,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(40.dp),
                tint = Color.White
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(text = title, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
            }
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5856D6))
            ) {
                Text(buttonText)
            }
        }
    }
}

@Composable
fun PremiumPromoCard(onNavigateToPremium: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onNavigateToPremium),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = "Premium",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Go Premium!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = Color.White
                )
                Text(
                    text = "Unlock ad-free experience, unlimited history, and exclusive features!",
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
