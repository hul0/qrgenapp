package com.hulo.qrgenapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.nativead.NativeAd

// This is a UI-level helper to organize the list. It does not contain business logic.
private sealed interface HistoryListItem {
    data class Header(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) : HistoryListItem
    data class PremiumPrompt(val onNavigate: () -> Unit) : HistoryListItem
    data class HistoryItem(val text: String) : HistoryListItem
    data class AdItem(val nativeAd: NativeAd?) : HistoryListItem
    object EmptyState : HistoryListItem
    data class ClearButton(val onClear: () -> Unit) : HistoryListItem
}

@Composable
fun HistoryScreen(
    userViewModel: UserViewModel,
    onNavigateToPremium: () -> Unit,
    showToast: (String) -> Unit,
    onShowInterstitialAd: () -> Unit,
    nativeAd: NativeAd?,
    showAd: Boolean
) {
    // This logic remains untouched.
    val uiState by userViewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    // This is a UI-level list construction. All logic calls are preserved from your original file.
    val listItems = buildList<HistoryListItem> {
        add(HistoryListItem.Header("Scan History", Icons.Default.History))
        if (!uiState.isPremium) {
            add(HistoryListItem.PremiumPrompt(onNavigateToPremium))
        }
        if (uiState.scanHistory.isEmpty()) {
            add(HistoryListItem.EmptyState)
        } else {
            val historyWithAd = uiState.scanHistory.map<String, HistoryListItem> { HistoryListItem.HistoryItem(it) }.toMutableList()
            // The ad is inserted as a UI element in the list.
            if (showAd) {
                val adPosition = 4.coerceAtMost(historyWithAd.size)
                historyWithAd.add(adPosition, HistoryListItem.AdItem(nativeAd))
            }
            addAll(historyWithAd)
            add(HistoryListItem.ClearButton {
                userViewModel.clearScanHistory()
                showToast("History cleared!")
            })
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2E3192), Color(0xFF1B1464))
                )
            )
    ) {
        // The entire screen is now a single LazyColumn for seamless scrolling.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentPadding = PaddingValues(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(
                items = listItems,
                key = { item ->
                    when (item) {
                        is HistoryListItem.HistoryItem -> item.text + System.nanoTime()
                        else -> item.javaClass.simpleName
                    }
                }
            ) { item ->
                // Render the correct UI component based on its type in the list.
                when (item) {
                    is HistoryListItem.Header -> ScreenHeader(item.title, item.icon)
                    is HistoryListItem.PremiumPrompt -> PremiumPromoCard(item.onNavigate, Modifier.padding(16.dp))
                    is HistoryListItem.HistoryItem -> HistoryItemCard(
                        scanResult = item.text,
                        onCopyClick = {
                            // Logic is preserved.
                            clipboardManager.setText(AnnotatedString(item.text))
                            showToast("Copied to clipboard!")
                            if (!uiState.isPremium) {
                                onShowInterstitialAd()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    is HistoryListItem.AdItem -> NativeAdViewComposable(
                        nativeAd = item.nativeAd,
                        showAd = true,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    is HistoryListItem.EmptyState -> EmptyHistoryView(Modifier.fillParentMaxHeight(0.7f))
                    is HistoryListItem.ClearButton -> ClearHistoryButton(item.onClear)
                }
                // Add spacing between items.
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// --- UI-Only Components Below ---

@Composable
private fun ScreenHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(50.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun HistoryItemCard(scanResult: String, onCopyClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = scanResult,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier.weight(1f),
                maxLines = 3
            )
            IconButton(onClick = onCopyClick) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy to Clipboard",
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun PremiumPromoCard(onNavigateToPremium: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onNavigateToPremium),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
                    )
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = "Premium",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
            Text(
                "Unlock Unlimited History",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White
            )
            Text(
                "Go Premium to save all your scans, remove ads, and more!",
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EmptyHistoryView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = "No History",
            modifier = Modifier.size(80.dp),
            tint = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No Scans Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            "Your scanned QR codes will appear here.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ClearHistoryButton(onClear: () -> Unit) {
    Button(
        onClick = onClear,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        Icon(Icons.Default.DeleteForever, contentDescription = "Clear History")
        Spacer(modifier = Modifier.width(8.dp))
        Text("Clear History")
    }
}
