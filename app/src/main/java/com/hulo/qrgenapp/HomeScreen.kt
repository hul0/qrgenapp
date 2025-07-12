package com.hulo.qrgenapp

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.nativead.NativeAd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    coinBalance: Int,
    diamondBalance: Int, // New: Diamond balance
    isPremiumUser: Boolean, // New: Premium status
    onNavigateToGenerate: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToGainCoins: () -> Unit,
    onNavigateToRedeem: () -> Unit, // New: Navigate to redeem screen
    onNavigateToHistory: () -> Unit, // New: Navigate to history screen
    onNavigateToPremium: () -> Unit, // New: Navigate to premium screen
    nativeAd: NativeAd?,
    showNativeAd: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp), // Apply horizontal padding to the whole column
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Align content to the top
    ) {
        // Top Spacer for visual breathing room
        Spacer(modifier = Modifier.height(24.dp))

        // App Header Section - Minimalistic and clear
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp), // Add padding below the header
            shape = RoundedCornerShape(24.dp), // More rounded for a softer look
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), // A slightly elevated surface for the header
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp), // Generous padding inside the header card
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "App Logo",
                    modifier = Modifier.size(100.dp), // Slightly smaller for a more compact header
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Welcome to QRWiz!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your all-in-one solution for generating and scanning QR codes with ease and advanced features.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp) // Reduced horizontal padding for description
                )
            }
        }

        // Feature Cards Section - Clearly grouped
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp) // Padding for section title
        )
        FeatureCard(
            icon = Icons.AutoMirrored.Filled.Send,
            title = "Generate QR Codes",
            description = "Create custom QR codes for text, URLs, and more. Costs coins per generation based on complexity.",
            onClick = onNavigateToGenerate,
            tooltipText = "Tap to generate new QR codes"
        )
        Spacer(modifier = Modifier.height(12.dp)) // Reduced spacing between feature cards
        FeatureCard(
            icon = Icons.Default.QrCodeScanner,
            title = "Scan Any QR Code",
            description = "Quickly scan QR codes from your camera. Earn 5 coins per scan (with internet)!",
            onClick = onNavigateToScan,
            tooltipText = "Tap to scan QR codes"
        )
        Spacer(modifier = Modifier.height(12.dp))
        FeatureCard(
            icon = Icons.Default.MonetizationOn,
            title = "Earn More Coins",
            description = "Watch rewarded ads to gain more coins!",
            onClick = onNavigateToGainCoins,
            tooltipText = "Tap to earn more coins"
        )
        Spacer(modifier = Modifier.height(12.dp))
        FeatureCard(
            icon = Icons.Default.CardGiftcard,
            title = "Redeem Codes",
            description = "Enter special codes to earn Diamonds!",
            onClick = onNavigateToRedeem,
            tooltipText = "Tap to redeem codes"
        )
        Spacer(modifier = Modifier.height(12.dp))
        FeatureCard(
            icon = Icons.Default.History,
            title = "Scan History",
            description = if (isPremiumUser) "View all your past scans (Unlimited)." else "View your recent scan history (Limited for free users).",
            onClick = onNavigateToHistory,
            tooltipText = "Tap to view scan history"
        )
        Spacer(modifier = Modifier.height(24.dp)) // Spacer before coin balance

        // Premium Section
        if (!isPremiumUser) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Premium",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Go Premium!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Unlock an ad-free experience, unlimited history, and more!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToPremium,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Learn More & Buy")
                    }
                }
            }
        }

        // Coin and Diamond Balance Section - Prominent and distinct
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp), // Padding below the coin balance card
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), // Distinct color for balance
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround // Use space around for two items
            ) {
                // Coin Balance
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Coin Balance",
                        modifier = Modifier.size(36.dp), // Slightly larger icon
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp)) // Increased spacing
                    Text(
                        text = "$coinBalance Coins",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
                // Diamond Balance
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = "Diamond Balance",
                        modifier = Modifier.size(36.dp), // Slightly larger icon
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(12.dp)) // Increased spacing
                    Text(
                        text = "$diamondBalance Diamonds",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Dedicated Native Ad Section
        NativeAdViewComposable(
            nativeAd = nativeAd,
            showAd = showNativeAd
        )

        // Bottom Spacer
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureCard(icon: ImageVector, title: String, description: String, onClick: () -> Unit, tooltipText: String) {
    var showTooltip by remember { mutableStateOf(false) }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            if (showTooltip) {
                PlainTooltip {
                    Text(tooltipText)
                }
            }
        },
        state = rememberTooltipState()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick) // Use clickable for ripple effect
                .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)), // Animation for size changes
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Reduced elevation for a flatter look
            shape = RoundedCornerShape(16.dp), // Slightly less rounded for a cleaner edge
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) // Lighter surface color
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp), // Slightly smaller icon for minimalism
                    tint = MaterialTheme.colorScheme.primary // Primary color for icons
                )
                Spacer(modifier = Modifier.width(16.dp)) // Standard spacing
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp)) // Standard spacing
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
