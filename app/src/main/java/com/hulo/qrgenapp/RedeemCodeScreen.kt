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
    showNativeAd: Boolean // New: Control native ad visibility
) {
    val uiState by userViewModel.uiState.collectAsState()
    var redeemCodeInput by remember { mutableStateOf(TextFieldValue("")) }
    var redeemMessage by remember { mutableStateOf("") }

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

            Spacer(modifier = Modifier.height(8.dp)) // Small spacer after ad
        }

        Spacer(modifier = Modifier.height(16.dp)) // Smaller top spacer

        Icon(
            imageVector = Icons.Default.Diamond,
            contentDescription = "Redeem Diamonds",
            modifier = Modifier.size(80.dp), // Smaller icon
            tint = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.height(12.dp)) // Smaller spacer
        Text(
            text = "Redeem Your Code",
            style = MaterialTheme.typography.headlineSmall, // Smaller headline
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp)) // Smaller spacer
        Text(
            text = "Enter a special redeem code to earn Diamonds!",
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
                OutlinedTextField(
                    value = redeemCodeInput,
                    onValueChange = { redeemCodeInput = it },
                    label = { Text("Enter Redeem Code", style = MaterialTheme.typography.bodySmall) }, // Smaller label
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp), // Slightly less rounded
                    leadingIcon = { Icon(Icons.Filled.CardGiftcard, contentDescription = "Redeem Code", modifier = Modifier.size(20.dp)) } // Smaller icon
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
                        .height(50.dp), // Smaller button height
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)), // Transparent feel
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp) // Smaller elevation
                ) {
                    Text("REDEEM CODE", style = MaterialTheme.typography.titleSmall) // Smaller text
                }

                if (redeemMessage.isNotBlank()) {
                    Text(
                        text = redeemMessage,
                        style = MaterialTheme.typography.bodyMedium, // Smaller text
                        color = if (redeemMessage.contains("Successfully")) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // Smaller spacer

        // Native Ad Section
        NativeAdViewComposable(
            nativeAd = nativeAd,
            showAd = showNativeAd
        )
        Spacer(modifier = Modifier.height(24.dp)) // Small spacer after ad

        // Current Diamond Balance Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp), // Smaller horizontal padding
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)), // Transparent feel
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), // Smaller padding
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = "Current Diamond Balance",
                    modifier = Modifier.size(28.dp), // Smaller icon
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(6.dp)) // Smaller spacer
                Text(
                    text = "Your Current Diamonds: ${uiState.diamonds}",
                    style = MaterialTheme.typography.titleMedium, // Smaller text
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp)) // Smaller bottom spacer
    }
}
