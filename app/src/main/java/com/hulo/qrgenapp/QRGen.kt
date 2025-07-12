package com.hulo.qrgenapp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.nativead.NativeAd
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

// Ad Unit ID for banner ad on this screen
private const val BANNER_AD_UNIT_ID_GENERATE_SCREEN = "ca-app-pub-3940256099942544/6300978111" // Google's Test Banner Ad Unit ID

data class QRGenUiState(
    val inputText: TextFieldValue = TextFieldValue(""),
    val generatedBitmap: Bitmap? = null,
    val isGenerating: Boolean = false,
    val errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.L,
    val snackbarMessage: String? = null
)

class QRGenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(QRGenUiState())
    val uiState: StateFlow<QRGenUiState> = _uiState.asStateFlow()

    fun onInputTextChanged(newText: TextFieldValue) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun onErrorCorrectionLevelChanged(level: ErrorCorrectionLevel) {
        _uiState.update { it.copy(errorCorrectionLevel = level) }
    }

    fun generateQRCode(onDeductCoins: (Int) -> Boolean, showToast: (String) -> Unit) {
        if (_uiState.value.inputText.text.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Input text cannot be empty.") }
            return
        }

        val generationCost = QRGenerator.getGenerationCost(_uiState.value.errorCorrectionLevel)
        if (!onDeductCoins(generationCost)) {
            showToast("Not enough coins! You need $generationCost coins to generate a QR code with this error correction level.")
            return
        }

        _uiState.update { it.copy(isGenerating = true) }

        viewModelScope.launch {
            val config = QRCodeConfig(
                content = _uiState.value.inputText.text,
                errorCorrectionLevel = _uiState.value.errorCorrectionLevel
            )
            val bitmap = QRGenerator.generateQRCode(config)
            _uiState.update { it.copy(generatedBitmap = bitmap, isGenerating = false) }
            if (bitmap != null) {
                showToast("QR Code generated successfully! $generationCost coins deducted.")
            } else {
                showToast("Failed to generate QR Code.")
            }
        }
    }

    fun saveQRCode(context: Context) {
        val bitmap = _uiState.value.generatedBitmap
        if (bitmap == null) {
            _uiState.update { it.copy(snackbarMessage = "No QR Code to save.") }
            return
        }

        viewModelScope.launch {
            val displayName = "QRCode_${System.currentTimeMillis()}"
            QRGenerator.saveQRCodeToGallery(context, bitmap, displayName)
            _uiState.update { it.copy(snackbarMessage = "Save operation completed.") }
        }
    }

    fun shareQRCode(context: Context) {
        val bitmap = _uiState.value.generatedBitmap
        if (bitmap == null) {
            _uiState.update { it.copy(snackbarMessage = "No QR Code to share.") }
            return
        }

        viewModelScope.launch {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val stream = FileOutputStream("$cachePath/image.png")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val imageFile = File(cachePath, "image.png")
            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)

            if (contentUri != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, "Generated QR Code for: ${_uiState.value.inputText.text}")
                }
                launch(Dispatchers.Main) {
                    context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
                }
            }
        }
    }

    fun snackbarMessageShown() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRGenScreen(
    viewModel: QRGenViewModel,
    coinBalance: Int, // Pass coin balance
    onDeductCoins: (Int) -> Boolean, // Pass deduct coins function
    onShowInterstitialAd: () -> Unit,
    showToast: (String) -> Unit, // Callback for showing toasts
    nativeAd: NativeAd?, // New: Native Ad
    showNativeAd: Boolean // New: Control native ad visibility
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.snackbarMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background) // Ensure background matches theme
                .padding(12.dp) // Overall smaller padding
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp) // Smaller spacing
        ) {
            // Banner Ad at the top of the screen
            if (showNativeAd) { // showNativeAd is true if not premium
                BannerAd(
                    adUnitId = BANNER_AD_UNIT_ID_GENERATE_SCREEN,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp)) // Small spacer after ad
            }

            Text(
                text = "QR Code Generator",
                style = MaterialTheme.typography.headlineSmall, // Smaller headline
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Display current coin balance
            Text(
                text = "Current Coins: $coinBalance",
                style = MaterialTheme.typography.titleSmall, // Smaller text
                color = MaterialTheme.colorScheme.secondary
            )

            QRCodeDisplay(
                bitmap = uiState.generatedBitmap,
                isGenerating = uiState.isGenerating
            )

            QRInputSection(
                textValue = uiState.inputText,
                onTextChanged = viewModel::onInputTextChanged,
                onGenerateClick = {
                    onShowInterstitialAd() // Show interstitial ad before generation
                    viewModel.generateQRCode(onDeductCoins, showToast) // Pass coin logic
                },
                currentErrorCorrectionLevel = uiState.errorCorrectionLevel // Pass current level
            )

            AdvancedOptions(
                selectedLevel = uiState.errorCorrectionLevel,
                onLevelSelected = viewModel::onErrorCorrectionLevelChanged
            )

            ActionButtons(
                isEnabled = uiState.generatedBitmap != null,
                onSaveClick = { viewModel.saveQRCode(context) },
                onShareClick = { viewModel.shareQRCode(context) }
            )

            // Native Ad Section
            NativeAdViewComposable(
                nativeAd = nativeAd,
                showAd = showNativeAd
            )
        }
    }
}

@Composable
fun QRCodeDisplay(bitmap: Bitmap?, isGenerating: Boolean) {
    Box(
        modifier = Modifier
            .size(240.dp) // Smaller display size
            .clip(RoundedCornerShape(16.dp)) // Slightly less rounded
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) // Transparent feel
            .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(16.dp)), // Transparent border
        contentAlignment = Alignment.Center
    ) {
        if (isGenerating) {
            CircularProgressIndicator(modifier = Modifier.size(56.dp), strokeWidth = 5.dp) // Smaller indicator
        } else if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Generated QR Code",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp) // Smaller padding
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.QrCode,
                    contentDescription = "QR Code Placeholder",
                    modifier = Modifier.size(64.dp), // Smaller icon
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) // More transparent
                )
                Spacer(modifier = Modifier.height(6.dp)) // Smaller spacer
                Text(
                    text = "Your QR Code will appear here",
                    style = MaterialTheme.typography.bodyMedium, // Smaller text
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun QRInputSection(
    textValue: TextFieldValue,
    onTextChanged: (TextFieldValue) -> Unit,
    onGenerateClick: () -> Unit,
    currentErrorCorrectionLevel: ErrorCorrectionLevel // New parameter
) {
    val generationCost = QRGenerator.getGenerationCost(currentErrorCorrectionLevel)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp) // Smaller spacing
    ) {
        OutlinedTextField(
            value = textValue,
            onValueChange = onTextChanged,
            label = { Text("Enter text or URL to encode", style = MaterialTheme.typography.bodySmall) }, // Smaller label
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 4, // Reduced max lines
            shape = RoundedCornerShape(10.dp), // Slightly less rounded
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = "Input", modifier = Modifier.size(20.dp)) } // Smaller icon
        )
        Button(
            onClick = onGenerateClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp), // Smaller button height
            shape = RoundedCornerShape(12.dp), // Slightly less rounded
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)), // Transparent feel
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp) // Smaller elevation
        ) {
            Text("GENERATE QR CODE ($generationCost Coins)", style = MaterialTheme.typography.titleSmall) // Smaller text
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedOptions(
    selectedLevel: ErrorCorrectionLevel,
    onLevelSelected: (ErrorCorrectionLevel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val levels = remember { getErrorCorrectionLevels() }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = getErrorCorrectionLevelName(selectedLevel),
            onValueChange = {},
            readOnly = true,
            label = { Text("Error Correction Level", style = MaterialTheme.typography.bodySmall) }, // Smaller label
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = "Settings", modifier = Modifier.size(20.dp)) }, // Smaller icon
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp) // Slightly less rounded
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            levels.forEach { level ->
                DropdownMenuItem(
                    text = { Text("${getErrorCorrectionLevelName(level)} - Cost: ${QRGenerator.getGenerationCost(level)} Coins", style = MaterialTheme.typography.bodyMedium) }, // Smaller text
                    onClick = {
                        onLevelSelected(level)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ActionButtons(
    isEnabled: Boolean,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp), // Smaller spacing
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onSaveClick,
            enabled = isEnabled,
            modifier = Modifier
                .weight(1f)
                .height(48.dp), // Smaller button height
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)), // Transparent feel
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp) // Smaller elevation
        ) {
            Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(20.dp)) // Smaller icon
            Spacer(Modifier.width(6.dp)) // Smaller spacer
            Text("Save", style = MaterialTheme.typography.titleSmall) // Smaller text
        }

        Button(
            onClick = onShareClick,
            enabled = isEnabled,
            modifier = Modifier
                .weight(1f)
                .height(48.dp), // Smaller button height
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)), // Transparent feel
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp) // Smaller elevation
        ) {
            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp)) // Smaller icon
            Spacer(Modifier.width(6.dp)) // Smaller spacer
            Text("Share", style = MaterialTheme.typography.titleSmall) // Smaller text
        }
    }
}
