package com.hulo.qrgenapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.nativead.NativeAd

@Composable
fun RedeemCodeScreen(
    userViewModel: UserViewModel,
    showToast: (String) -> Unit,
    nativeAd: NativeAd?,
    showAd: Boolean
) {
    // This logic remains untouched.
    val uiState by userViewModel.uiState.collectAsState()
    var redeemCodeInput by remember { mutableStateOf(TextFieldValue("")) }
    var redeemMessage by remember { mutableStateOf("") }

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
                imageVector = Icons.Default.CardGiftcard,
                contentDescription = "Redeem Code",
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF81D4FA)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Redeem Code",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Enter a special code to earn Diamonds!",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Redeem Card
            RedeemCard(
                redeemCodeInput = redeemCodeInput,
                onValueChange = { redeemCodeInput = it },
                onRedeemClick = {
                    // This logic call is preserved.
                    if (redeemCodeInput.text.isNotBlank()) {
                        redeemMessage = userViewModel.redeemCode(redeemCodeInput.text)
                        showToast(redeemMessage)
                        redeemCodeInput = TextFieldValue("")
                    } else {
                        showToast("Please enter a redeem code.")
                    }
                }
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Store Section (New UI Feature)
            StoreSection()
        }
    }
}

@Composable
private fun RedeemCard(
    redeemCodeInput: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onRedeemClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = redeemCodeInput,
                onValueChange = onValueChange,
                label = { Text("Enter Redeem Code", color = Color.White.copy(alpha = 0.7f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.8f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                )
            )
            Button(
                onClick = onRedeemClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5856D6))
            ) {
                Text("REDEEM", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun StoreSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(Icons.Default.Storefront, contentDescription = "Store", tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Store",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
        ) {
            // Glassmorphic background for the whole store section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(16.dp)
                    .alpha(0.5f), // Make content semi-transparent to emphasize "Coming Soon"
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StoreItem(
                    icon = Icons.Default.Diamond,
                    title = "1000 Diamonds",
                    price = "$1.99",
                    iconColor = Color(0xFF81D4FA)
                )
                StoreItem(
                    icon = Icons.Default.MonetizationOn,
                    title = "5000 Coins",
                    price = "$0.99",
                    iconColor = Color(0xFFFFD700)
                )
            }
            // "Coming Soon" overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Coming Soon",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StoreItem(icon: ImageVector, title: String, price: String, iconColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
        Button(onClick = { /* Disabled */ }, enabled = false, shape = RoundedCornerShape(12.dp)
            ) {
            Text(price , color = Color.White)
        }
    }
}
