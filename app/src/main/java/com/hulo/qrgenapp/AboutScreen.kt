package com.hulo.qrgenapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.nativead.NativeAd

// Ad Unit ID for banner ad on this screen
private const val BANNER_AD_UNIT_ID_ABOUT_SCREEN = "ca-app-pub-3940256099942544/6300978111" // Google's Test Banner Ad Unit ID

@Composable
fun AboutScreen(
    nativeAd: NativeAd?, // New: Native Ad
    showNativeAd: Boolean // New: Control native ad visibility
) {
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
            imageVector = Icons.Default.Info,
            contentDescription = "About App",
            modifier = Modifier.size(80.dp), // Smaller icon
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp)) // Smaller spacer
        Text(
            text = "About QR Code Scanner & Generator - QRWiz", // Updated app name
            style = MaterialTheme.typography.headlineSmall, // Smaller headline
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center, // Centered for better presentation
            modifier = Modifier.padding(horizontal = 12.dp) // Smaller horizontal padding
        )
        Spacer(modifier = Modifier.height(6.dp)) // Smaller spacer
        Text(
            text = "Version 1.0.0",
            style = MaterialTheme.typography.titleSmall, // Smaller text
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp)) // Smaller spacer

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp), // Smaller horizontal padding
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // Slightly increased elevation
            shape = RoundedCornerShape(16.dp), // More rounded corners
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)) // Transparent feel
        ) {
            Column(modifier = Modifier.padding(16.dp)) { // Smaller padding
                Text(
                    text = "QR Code Scanner & Generator - QRWiz is a versatile and user-friendly application designed to simplify your QR code interactions. Whether you need to quickly generate a QR code for sharing information or efficiently scan one to access content, our app provides a seamless experience.",
                    style = MaterialTheme.typography.bodyMedium, // Smaller text
                    textAlign = TextAlign.Justify,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp)) // Smaller spacer
                Text(
                    text = "Key Features:",
                    style = MaterialTheme.typography.titleMedium, // Smaller title
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp)) // Smaller spacer
                BulletPointText(text = "Fast and accurate QR code scanning.")
                BulletPointText(text = "Customizable QR code generation (text, URL, etc.).")
                BulletPointText(text = "Save and share generated QR codes.")
                BulletPointText(text = "Gamified experience with coin rewards for scanning and ad views.")
                Spacer(modifier = Modifier.height(12.dp)) // Smaller spacer
                Text(
                    text = "Developed by Rupam Ghosh with ❤️ using Jetpack Compose and Google's latest technologies.", // Added developer name
                    style = MaterialTheme.typography.bodySmall, // Smaller text
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp)) // Smaller spacer
                Text(
                    text = "Contact: nmrupam@proton.me", // Added contact email
                    style = MaterialTheme.typography.bodySmall, // Smaller text
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp)) // Smaller spacer

                // Attribution Section
                Text(
                    text = "Attributions:",
                    style = MaterialTheme.typography.titleMedium, // Smaller title
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp)) // Smaller spacer
                Text(
                    text = "The app icon for QR Code Scanner & Generator - QRWiz was designed using resources from Freepik.com. We extend our sincere gratitude to Freepik for providing high-quality assets that helped enhance our app's visual appeal.",
                    style = MaterialTheme.typography.bodySmall, // Smaller text
                    textAlign = TextAlign.Justify,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Icon by Freepik (www.freepik.com)", // Direct attribution link if possible, or just the site
                    style = MaterialTheme.typography.bodySmall, // Smaller text
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
fun BulletPointText(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "•",
            modifier = Modifier.padding(end = 6.dp), // Smaller padding
            style = MaterialTheme.typography.bodyMedium, // Smaller text
            color = MaterialTheme.colorScheme.primary // Bullet point in primary color
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium, // Smaller text
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
