package com.hulo.qrgenapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick // Corrected import
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.nativead.NativeAd

// Ad Unit ID for banner ad on this screen
private const val BANNER_AD_UNIT_ID_PREMIUM_SCREEN = "ca-app-pub-3940256099942544/6300978111" // Google's Test Banner Ad Unit ID

@Composable
fun PremiumPlanScreen(
    userViewModel: UserViewModel,
    showToast: (String) -> Unit,
    nativeAd: NativeAd?, // New: Native Ad
    showAd: Boolean // New: Control native ad visibility (renamed from showNativeAd for consistency)
) {
    val uiState by userViewModel.uiState.collectAsState()
    val isPremium = uiState.isPremium
    val diamonds = uiState.diamonds
    val premiumCost = 1000 // Defined in UserViewModel.kt, but hardcoding for UI display

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp), // Overall horizontal padding
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
            imageVector = Icons.Default.Star,
            contentDescription = "Premium Plan",
            modifier = Modifier.size(96.dp), // Larger icon
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp)) // Increased spacer
        Text(
            text = "QRWiz Premium",
            style = MaterialTheme.typography.headlineMedium, // Larger headline
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp)) // Increased spacer
        Text(
            text = "Unlock the full potential of QRWiz with our exclusive Premium plan!",
            style = MaterialTheme.typography.bodyLarge, // Larger text
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp) // Increased horizontal padding
        )
        Spacer(modifier = Modifier.height(32.dp)) // Increased spacer

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp), // Increased horizontal padding
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), // Increased elevation
            shape = RoundedCornerShape(20.dp), // More rounded
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f)) // Less transparent, more solid
        ) {
            Column(
                modifier = Modifier.padding(20.dp), // Increased padding
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp) // Increased spacing
            ) {
                Text(
                    text = "Benefits of Premium:",
                    style = MaterialTheme.typography.titleLarge, // Larger title
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp)) // Increased spacer

                PremiumFeatureItem(
                    title = "Ad-Free Experience",
                    description = "Enjoy the app without any banner, interstitial, or native ads.",
                    icon = Icons.Default.AdsClick // Corrected usage
                )
                PremiumFeatureItem(
                    title = "Unlimited Scan History",
                    description = "Keep track of all your past scans without any limits.",
                    icon = Icons.Default.History
                )
                // Add more premium features here if needed
                // PremiumFeatureItem("Priority Support", "Get faster assistance from our team.", Icons.Default.Support)

                Spacer(modifier = Modifier.height(24.dp)) // Increased spacer

                if (isPremium) {
                    Text(
                        text = "You are already a Premium user! Enjoy the benefits.",
                        style = MaterialTheme.typography.titleLarge, // Larger title
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "Cost: $premiumCost Diamonds",
                        style = MaterialTheme.typography.headlineSmall, // Larger headline
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp)) // Increased spacer
                    Text(
                        text = "Your Current Diamonds: $diamonds",
                        style = MaterialTheme.typography.titleMedium, // Larger text
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp)) // Increased spacer
                    Button(
                        onClick = {
                            val success = userViewModel.buyPremium()
                            if (success) {
                                showToast("Congratulations! You are now a Premium user!")
                            } else {
                                showToast("Not enough Diamonds. You need $premiumCost Diamonds to buy Premium.")
                            }
                        },
                        enabled = diamonds >= premiumCost,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp), // Larger button height
                        shape = RoundedCornerShape(16.dp), // More rounded
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), // Solid primary color
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp) // Increased elevation
                    ) {
                        Icon(Icons.Default.Diamond, contentDescription = "Buy Premium", modifier = Modifier.size(24.dp)) // Larger icon
                        Spacer(Modifier.width(8.dp)) // Increased spacer
                        Text("BUY PREMIUM", style = MaterialTheme.typography.titleLarge) // Larger text
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp)) // Increased bottom spacer
    }
}

@Composable
fun PremiumFeatureItem(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp), // Larger icon
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(16.dp)) // Increased spacer
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge, // Larger title
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge, // Larger text
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
