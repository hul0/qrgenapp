package com.hulo.qrgenapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.nativead.NativeAd

@Composable
fun RedeemCodeScreen(
    userViewModel: UserViewModel,
    showToast: (String) -> Unit,
    nativeAd: NativeAd?, // New: Native Ad
    showAd: Boolean // New: Control native ad visibility (renamed from showNativeAd for consistency)
) {
    val uiState by userViewModel.uiState.collectAsState()
    var redeemCodeInput by remember { mutableStateOf(TextFieldValue("")) }
    var redeemMessage by remember { mutableStateOf("") }

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
            imageVector = Icons.Default.Diamond,
            contentDescription = "Redeem Diamonds",
            modifier = Modifier.size(96.dp), // Larger icon
            tint = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.height(16.dp)) // Increased spacer
        Text(
            text = "Redeem Your Code",
            style = MaterialTheme.typography.headlineMedium, // Larger headline
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp)) // Increased spacer
        Text(
            text = "Enter a special redeem code to earn Diamonds!",
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
                OutlinedTextField(
                    value = redeemCodeInput,
                    onValueChange = { redeemCodeInput = it },
                    label = { Text("Enter Redeem Code", style = MaterialTheme.typography.bodyLarge) }, // Larger label
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp), // More rounded
                    leadingIcon = { Icon(Icons.Filled.CardGiftcard, contentDescription = "Redeem Code", modifier = Modifier.size(24.dp)) } // Larger icon
                )

                Button(
                    onClick = {
                        if (redeemCodeInput.text.isNotBlank()) {
                            redeemMessage = userViewModel.redeemCode(redeemCodeInput.text)
                            showToast(redeemMessage)
                            redeemCodeInput = TextFieldValue("") // Clear input after attempt
                        } else {
                            showToast("Please enter a redeem code.")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp), // Larger button height
                    shape = RoundedCornerShape(16.dp), // More rounded
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), // Solid primary color
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp) // Increased elevation
                ) {
                    Text("REDEEM CODE", style = MaterialTheme.typography.titleLarge) // Larger text
                }

                if (redeemMessage.isNotBlank()) {
                    Text(
                        text = redeemMessage,
                        style = MaterialTheme.typography.bodyLarge, // Larger text
                        color = if (redeemMessage.contains("Successfully")) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp)) // Increased spacer

        // Current Diamond Balance Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp), // Increased horizontal padding
            shape = RoundedCornerShape(20.dp), // More rounded
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f)), // Less transparent, more solid
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // Increased elevation
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp), // Increased padding
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = "Current Diamond Balance",
                    modifier = Modifier.size(36.dp), // Larger icon
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp)) // Increased spacer
                Text(
                    text = "Your Current Diamonds: ${uiState.diamonds}",
                    style = MaterialTheme.typography.headlineSmall, // Larger text
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp)) // Increased bottom spacer
    }
}
