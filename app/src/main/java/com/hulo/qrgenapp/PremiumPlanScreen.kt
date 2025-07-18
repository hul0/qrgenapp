package com.hulo.qrgenapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.nativead.NativeAd

@Composable
fun PremiumPlanScreen(
    userViewModel: UserViewModel,
    showToast: (String) -> Unit,
    nativeAd: NativeAd?,
    showAd: Boolean
) {
    // This logic remains untouched.
    val uiState by userViewModel.uiState.collectAsState()
    val isPremium = uiState.isPremium
    val diamonds = uiState.diamonds
    val premiumCost = 1000

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2E3192), Color(0xFF1B1464))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = "Premium Plan",
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFFFD700)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "QRWiz Premium",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Unlock the full potential of your QR experience.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Benefits Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        "Premium Benefits",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    PremiumFeatureItem(
                        title = "Ad-Free Experience",
                        description = "Enjoy the app without any interruptions.",
                        icon = Icons.Default.AdsClick
                    )
                    PremiumFeatureItem(
                        title = "Unlimited Scan History",
                        description = "Keep a complete record of all your scans.",
                        icon = Icons.Default.History
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // Purchase Section
            if (isPremium) {
                Text(
                    text = "You are a Premium user!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    textAlign = TextAlign.Center
                )
            } else {
                PurchaseCard(
                    diamonds = diamonds,
                    premiumCost = premiumCost,
                    onBuyClick = {
                        // This logic call is preserved.
                        val success = userViewModel.buyPremium()
                        if (success) {
                            showToast("Congratulations! You are now a Premium user!")
                        } else {
                            showToast("Not enough Diamonds. You need $premiumCost Diamonds.")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PremiumFeatureItem(title: String, description: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(32.dp),
            tint = Color(0xFF81D4FA)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PurchaseCard(diamonds: Int, premiumCost: Int, onBuyClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Unlock with Diamonds", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Diamond, contentDescription = "Cost", tint = Color(0xFF81D4FA))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$premiumCost Diamonds",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                "Your balance: $diamonds Diamonds",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onBuyClick,
                enabled = diamonds >= premiumCost,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5856D6),
                    disabledContainerColor = Color.White.copy(alpha = 0.1f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("BUY PREMIUM", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
