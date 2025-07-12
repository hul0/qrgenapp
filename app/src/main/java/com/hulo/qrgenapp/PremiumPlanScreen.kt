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
    showNativeAd: Boolean // New: Control native ad visibility
) {
    val uiState by userViewModel.uiState.collectAsState()
    val isPremium = uiState.isPremium
    val diamonds = uiState.diamonds
    val premiumCost = 1000 // Defined in UserViewModel.kt, but hardcoding for UI display

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp), // Overall smaller horizontal padding
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Banner Ad at the top of the screen
        if (showNativeAd) { // showNativeAd is true if not premium
            BannerAd(
                adUnitId = BANNER_AD_UNIT_ID_PREMIUM_SCREEN,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp)) // Small spacer after ad
        }

        Spacer(modifier = Modifier.height(16.dp)) // Smaller top spacer

        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Premium Plan",
            modifier = Modifier.size(80.dp), // Smaller icon
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp)) // Smaller spacer
        Text(
            text = "QRWiz Premium",
            style = MaterialTheme.typography.headlineSmall, // Smaller headline
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp)) // Smaller spacer
        Text(
            text = "Unlock the full potential of QRWiz with our exclusive Premium plan!",
            style = MaterialTheme.typography.bodyMedium, // Smaller text
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp) // Smaller horizontal padding
        )
        Spacer(modifier = Modifier.height(24.dp)) // Smaller spacer

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp), // Smaller horizontal padding
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)) // Transparent feel
        ) {
            Column(
                modifier = Modifier.padding(16.dp), // Smaller padding
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp) // Smaller spacing
            ) {
                Text(
                    text = "Benefits of Premium:",
                    style = MaterialTheme.typography.titleMedium, // Smaller title
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp)) // Smaller spacer

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

                Spacer(modifier = Modifier.height(16.dp)) // Smaller spacer

                if (isPremium) {
                    Text(
                        text = "You are already a Premium user! Enjoy the benefits.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "Cost: $premiumCost Diamonds",
                        style = MaterialTheme.typography.headlineSmall, // Smaller headline
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp)) // Smaller spacer
                    Text(
                        text = "Your Current Diamonds: $diamonds",
                        style = MaterialTheme.typography.titleSmall, // Smaller text
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp)) // Smaller spacer
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
                            .height(50.dp), // Smaller button height
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)), // Transparent feel
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp) // Smaller elevation
                    ) {
                        Icon(Icons.Default.Diamond, contentDescription = "Buy Premium", modifier = Modifier.size(20.dp)) // Smaller icon
                        Spacer(Modifier.width(6.dp)) // Smaller spacer
                        Text("BUY PREMIUM", style = MaterialTheme.typography.titleSmall) // Smaller text
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp)) // Smaller bottom spacer

        // Native Ad Section
        NativeAdViewComposable(
            nativeAd = nativeAd,
            showAd = showNativeAd
        )
        Spacer(modifier = Modifier.height(16.dp)) // Small spacer after ad
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
            modifier = Modifier.size(24.dp), // Smaller icon
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(12.dp)) // Smaller spacer
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall, // Smaller title
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall, // Smaller text
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
