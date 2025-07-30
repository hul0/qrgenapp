package com.hulo.qrgenapp

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.nativead.NativeAd

data class QuickAction(
    val icon: ImageVector,
    val label: String,
    val action: () -> Unit
)

@SuppressLint("UnusedBoxWithConstraintsScope")
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
    nativeAd: NativeAd?,
    showNativeAd: Boolean
) {
    val actions = listOf(
        QuickAction(Icons.Filled.QrCodeScanner, "Scan QR", onNavigateToScan),
        QuickAction(Icons.Filled.Create, "Generate QR", onNavigateToGenerate),
        QuickAction(Icons.Filled.MonetizationOn, "Earn Coins", onNavigateToGainCoins),
        QuickAction(Icons.Filled.CardGiftcard, "Redeem Code", onNavigateToRedeem),
        QuickAction(Icons.Filled.History, "History", onNavigateToHistory),
        QuickAction(Icons.Filled.Star, "Premium", onNavigateToPremium)
    )

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
        // We use BoxWithConstraints to make decisions based on screen size.
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isPortrait = maxHeight > maxWidth

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (isPortrait) 32.dp else 16.dp), // Less padding on top in landscape
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Section
                Text(
                    "Welcome to QRWiz",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Your one-stop QR solution",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Balance Card
                BalanceCard(coinBalance, diamondBalance)
                Spacer(modifier = Modifier.height(24.dp))

                // Quick Actions Grid - Now contains the ad
                QuickActionGrid(
                    actions = actions,
                    nativeAd = nativeAd,
                    showNativeAd = showNativeAd && !isPremiumUser // Example: Hide ad for premium users
                )

                // **REMOVED** the ad from the bottom of the screen
            }
        }
    }
}

@Composable
fun BalanceCard(coinBalance: Int, diamondBalance: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // No elevation for glassmorphism
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            BalanceItem(Icons.Default.MonetizationOn, "Coins", coinBalance.toString(), Color(0xFFFFD700))
            // Vertical divider for visual separation
            Spacer(
                modifier = Modifier
                    .width(1.dp)
                    .height(60.dp)
                    .background(Color.White.copy(alpha = 0.2f))
            )
            BalanceItem(Icons.Default.Diamond, "Diamonds", diamondBalance.toString(), Color(0xFF81D4FA))
        }
    }
}

@Composable
fun BalanceItem(icon: ImageVector, label: String, value: String, tint: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(32.dp),
            tint = tint
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun QuickActionGrid(
    actions: List<QuickAction>,
    nativeAd: NativeAd?,
    showNativeAd: Boolean
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(actions) { action ->
                QuickActionButton(
                    icon = action.icon,
                    label = action.label,
                    onClick = action.action
                )
            }


        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    // Glassmorphism effect for the buttons
    Card(
        modifier = Modifier
            .aspectRatio(1f) // Make buttons square
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(40.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = Color.White
            )
        }
    }
}

