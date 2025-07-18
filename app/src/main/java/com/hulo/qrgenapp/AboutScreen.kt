package com.hulo.qrgenapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.nativead.NativeAd

@Composable
fun AboutScreen(
    nativeAd: NativeAd?,
    showNativeAd: Boolean
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2E3192), Color(0xFF1B1464))
                )
            )
    ) {
        // LazyColumn makes the whole screen scrollable, including the ad at the bottom.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About App",
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "About QRWiz",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Version 2.0.0",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Our Mission Card
            item {
                InfoCard(title = "Our Mission", icon = Icons.AutoMirrored.Filled.Label) {
                    Text(
                        text = "QRWiz is a versatile and user-friendly application designed to simplify your QR code interactions. Whether you need to quickly generate a QR code for sharing information or efficiently scan one to access content, our app provides a seamless experience.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Legal & Support Card
            item {
                InfoCard(title = "Support & Legal", icon = Icons.AutoMirrored.Filled.Help) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoRow(
                            icon = Icons.Default.Policy,
                            text = "Privacy Policy",
                            onClick = { openUrl(context, "https://hul0.github.io/qrwiz") }
                        )
                        Divider(color = Color.White.copy(alpha = 0.15f))
                        InfoRow(
                            icon = Icons.Default.ContactMail,
                            text = "Support",
                            onClick = { openUrl(context, "mailto:workspace@republicwing.com") }
                        )
                        Divider(color = Color.White.copy(alpha = 0.15f))
                        InfoRow(
                            icon = Icons.Default.StarRate,
                            text = "Rate this App",
                            onClick = { openUrl(context, "market://details?id=${context.packageName}") }
                        )
                    }
                }
            }

            // Developer Card
            item {
                InfoCard(title = "Developer", icon = Icons.Default.Person) {
                    Text(
                        text = "Developed with ❤️ Kotlin & Jetpack Compose \nContact: workspace@republicwing.com ",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Native Ad at the very bottom, styled to blend in.
            if (showNativeAd) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0f))
                    ) {
                        NativeAdViewComposable(nativeAd = nativeAd, showAd = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            content()
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Go to $text",
            tint = Color.White.copy(alpha = 0.7f)
        )
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle case where a browser or the Play Store is not available
        android.widget.Toast.makeText(context, "Could not open link.", android.widget.Toast.LENGTH_SHORT).show()
    }
}
