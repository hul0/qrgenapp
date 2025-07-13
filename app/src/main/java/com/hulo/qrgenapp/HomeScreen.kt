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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.nativead.NativeAd

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
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF8B5CF6),
                        Color(0xFF6366F1),
                        Color(0xFF4F46E5)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Banner Ad at the top of the screen
        if (showNativeAd) { // showNativeAd is true if not premium
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Welcome Section
        Text(
            text = "Hello, User!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "What would you like to do today?",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Balance Card with modern styling
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Your Balances",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BalanceItem(
                        icon = Icons.Default.MonetizationOn,
                        label = "Coins",
                        value = coinBalance.toString(),
                        tint = Color(0xFFEAB308)
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(60.dp)
                            .background(Color(0xFFE5E7EB))
                    )

                    BalanceItem(
                        icon = Icons.Default.Diamond,
                        label = "Diamonds",
                        value = diamondBalance.toString(),
                        tint = Color(0xFF3B82F6)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Quick Actions Section
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, bottom = 16.dp)
        )

        // Action buttons in a 2x3 grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.QrCodeScanner,
                    label = "Scan QR",
                    onClick = onNavigateToScan,
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color.White,
                    iconColor = Color(0xFF10B981)
                )
                QuickActionButton(
                    icon = Icons.Default.Create,
                    label = "Generate QR",
                    onClick = onNavigateToGenerate,
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color.White,
                    iconColor = Color(0xFF6366F1)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.MonetizationOn,
                    label = "Earn Coins",
                    onClick = onNavigateToGainCoins,
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color.White,
                    iconColor = Color(0xFFEAB308)
                )
                QuickActionButton(
                    icon = Icons.Default.CardGiftcard,
                    label = "Redeem Code",
                    onClick = onNavigateToRedeem,
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color.White,
                    iconColor = Color(0xFFEC4899)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.History,
                    label = "History",
                    onClick = onNavigateToHistory,
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color.White,
                    iconColor = Color(0xFF8B5CF6)
                )
                QuickActionButton(
                    icon = Icons.Default.Star,
                    label = "Premium",
                    onClick = onNavigateToPremium,
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFFFFD700),
                    iconColor = Color(0xFFB45309)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Native Ad Section
        NativeAdViewComposable(
            nativeAd = nativeAd,
            showAd = showNativeAd
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun BalanceItem(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(32.dp),
            tint = tint
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937)
        )
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    iconColor: Color = Color(0xFF6366F1)
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(40.dp),
                    tint = iconColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF1F2937)
                )
            }
        }
    }
}