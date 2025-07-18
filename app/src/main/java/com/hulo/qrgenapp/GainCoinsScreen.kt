package com.hulo.qrgenapp

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.nativead.NativeAd

// A sealed interface to better manage the different types of content in our list.
// This is a UI-level helper and does not contain business logic.
private sealed interface GainCoinsListItem {
    data class Header(val coinBalance: Int) : GainCoinsListItem
    data class DailyBonus(val amount: Int, val streak: Int, val onClaim: () -> Unit) : GainCoinsListItem
    data class DailyStreakView(val pattern: List<Int>, val currentStreak: Int, val isBonusAvailable: Boolean) : GainCoinsListItem
    data class EarningMethod(val title: String, val description: String, val icon: ImageVector, val buttonText: String, val onClick: () -> Unit) : GainCoinsListItem
    data class Ad(val nativeAd: NativeAd?) : GainCoinsListItem
    object PremiumPromo : GainCoinsListItem
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
    // This is a UI-level list construction. All logic calls are preserved.
    val screenItems = buildList<GainCoinsListItem> {
        add(GainCoinsListItem.Header(coinBalance))
        if (dailyBonusAvailable) {
            add(GainCoinsListItem.DailyBonus(dailyBonusAmount, dailyStreak, onClaimDailyBonus))
        }
        add(GainCoinsListItem.DailyStreakView(dailyBonusPattern, dailyStreak, dailyBonusAvailable))

        add(GainCoinsListItem.EarningMethod(
            title = "Watch an Ad",
            description = "Get +50 coins for each video you watch.",
            icon = Icons.Default.Videocam,
            buttonText = "Watch Now",
            onClick = { onShowRewardedAd { /* handled in MainActivity */ } }
        ))

        // --- NATIVE AD INTEGRATION ---
        // The ad is now part of the list of earning methods.


        add(GainCoinsListItem.EarningMethod(
            title = "Scan QR Codes",
            description = "Earn +5 coins for every successful scan.",
            icon = Icons.Default.QrCodeScanner,
            buttonText = "Start Scanning",
            onClick = onNavigateToScan
        ))

        if (!isPremiumUser) {
            add(GainCoinsListItem.PremiumPromo)
        }
        if (showNativeAd) {
            add(GainCoinsListItem.Ad(nativeAd))
        }
    }

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
            modifier = Modifier.fillMaxSize().systemBarsPadding(),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(
                items = screenItems,
                key = { item ->
                    when (item) {
                        is GainCoinsListItem.EarningMethod -> item.title
                        else -> item.javaClass.simpleName
                    }
                }
            ) { item ->
                when (item) {
                    is GainCoinsListItem.Header -> ScreenHeader(item.coinBalance)
                    is GainCoinsListItem.DailyBonus -> DailyBonusCard(item.amount, item.streak, item.onClaim)
                    is GainCoinsListItem.DailyStreakView -> DailyStreakProgressView(item.pattern, item.currentStreak, item.isBonusAvailable)
                    is GainCoinsListItem.EarningMethod -> EarningMethodCard(item.title, item.description, item.icon, item.buttonText, item.onClick)
                    is GainCoinsListItem.Ad -> NativeAdViewComposable(nativeAd = item.nativeAd, showAd = true, modifier = Modifier.padding(horizontal = 16.dp))
                    is GainCoinsListItem.PremiumPromo -> PremiumPromoCard(onNavigateToPremium)
                }
            }
        }
    }
}

@Composable
private fun ScreenHeader(coinBalance: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
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
private fun DailyBonusCard(amount: Int, streak: Int, onClaim: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClaim),
        shape = RoundedCornerShape(24.dp),
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
                .padding(24.dp)
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
          //  Box(modifier = Modifier.matchParentSize().shimmerBackground(RoundedCornerShape(24.dp)).alpha(0f))
        }
    }
}

@Composable
private fun DailyStreakProgressView(pattern: List<Int>, currentStreak: Int, isBonusAvailable: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("Your 7-Day Streak", fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))
        // --- RESPONSIVE FIX ---
        // LazyRow allows horizontal scrolling on small screens, preventing clipping.
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(items = pattern, key = { it }) { amount ->
                val dayIndex = pattern.indexOf(amount)
                val isClaimed = dayIndex < currentStreak
                val isClaimableToday = dayIndex == currentStreak && isBonusAvailable
                DailyRewardItem(day = dayIndex + 1, amount = amount, isClaimed = isClaimed, isClaimableToday = isClaimableToday)
            }
        }
    }
}

@Composable
private fun DailyRewardItem(day: Int, amount: Int, isClaimed: Boolean, isClaimableToday: Boolean) {
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
        else -> Color.White.copy(alpha = 0.2f)
    }
    val contentColor = if (isClaimed) Color.Black else Color.White

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isClaimed) {
                Icon(Icons.Default.Check, contentDescription = "Claimed", tint = contentColor)
            } else {
                Text("+$amount", fontWeight = FontWeight.Bold, color = contentColor, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("Day $day", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}

@Composable
private fun EarningMethodCard(
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
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
                Text(text = title, fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleMedium)
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
private fun PremiumPromoCard(onNavigateToPremium: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onNavigateToPremium),
        shape = RoundedCornerShape(24.dp),
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
                    imageVector = Icons.Default.WorkspacePremium,
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
                    text = "Unlock an ad-free experience and exclusive features!",
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// A custom shimmer animation modifier.
private fun Modifier.shimmerBackground(shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)): Modifier = composed {
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
        Color.White.copy(alpha = 0.3f),
        Color.White.copy(alpha = 0.1f),
        Color.White.copy(alpha = 0.3f)
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
