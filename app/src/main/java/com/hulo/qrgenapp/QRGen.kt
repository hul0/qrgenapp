package com.hulo.qrgenapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

// --- LOGIC SECTION: UNCHANGED FROM YOUR ORIGINAL FILE ---
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
        val bitmap = _uiState.value.generatedBitmap ?: return
        viewModelScope.launch {
            val displayName = "QRCode_${System.currentTimeMillis()}"
            QRGenerator.saveQRCodeToGallery(context, bitmap, displayName)
            _uiState.update { it.copy(snackbarMessage = "Save operation completed.") }
        }
    }

    fun shareQRCode(context: Context) {
        val bitmap = _uiState.value.generatedBitmap ?: return
        viewModelScope.launch {
            val cachePath = File(context.cacheDir, "images").apply { mkdirs() }
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
// --- END OF UNCHANGED LOGIC SECTION ---


@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRGenScreen(
    viewModel: QRGenViewModel,
    coinBalance: Int,
    onDeductCoins: (Int) -> Boolean,
    onShowInterstitialAd: () -> Unit,
    showToast: (String) -> Unit,
    nativeAd: NativeAd?,
    showNativeAd: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // This logic remains untouched.
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.snackbarMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF2E3192), Color(0xFF1B1464))
                    )
                )
                .padding(paddingValues)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isTallScreen = maxHeight > maxWidth

                if (isTallScreen) {
                    // Portrait layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PortraitContent(viewModel, uiState, coinBalance, onDeductCoins, onShowInterstitialAd, showToast, nativeAd, showNativeAd)
                    }
                } else {
                    // Landscape layout
                    LandscapeContent(viewModel, uiState, coinBalance, onDeductCoins, onShowInterstitialAd, showToast, nativeAd, showNativeAd)
                }
            }
        }
    }
}

// UI-only composable for Portrait mode
@Composable
private fun PortraitContent(
    viewModel: QRGenViewModel, uiState: QRGenUiState, coinBalance: Int,
    onDeductCoins: (Int) -> Boolean, onShowInterstitialAd: () -> Unit, showToast: (String) -> Unit,
    nativeAd: NativeAd?, showNativeAd: Boolean
) {
    val context = LocalContext.current
    QRCodeDisplay(
        bitmap = uiState.generatedBitmap,
        isGenerating = uiState.isGenerating,
        modifier = Modifier.fillMaxWidth(0.7f).aspectRatio(1f)
    )
    Spacer(modifier = Modifier.height(24.dp))
    QRInputAndOptions(viewModel, uiState, coinBalance, onDeductCoins, onShowInterstitialAd, showToast)
    Spacer(modifier = Modifier.height(16.dp))
    ActionButtons(
        isEnabled = uiState.generatedBitmap != null,
        onSaveClick = { viewModel.saveQRCode(context) },
        onShareClick = { viewModel.shareQRCode(context) }
    )
    if (showNativeAd) {
        Spacer(modifier = Modifier.height(16.dp))
        NativeAdViewComposable(nativeAd = nativeAd, showAd = true)
    }
}

// UI-only composable for Landscape mode
@Composable
private fun LandscapeContent(
    viewModel: QRGenViewModel, uiState: QRGenUiState, coinBalance: Int,
    onDeductCoins: (Int) -> Boolean, onShowInterstitialAd: () -> Unit, showToast: (String) -> Unit,
    nativeAd: NativeAd?, showNativeAd: Boolean
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            QRCodeDisplay(
                bitmap = uiState.generatedBitmap,
                isGenerating = uiState.isGenerating,
                modifier = Modifier.aspectRatio(1f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            ActionButtons(
                isEnabled = uiState.generatedBitmap != null,
                onSaveClick = { viewModel.saveQRCode(context) },
                onShareClick = { viewModel.shareQRCode(context) }
            )
        }
        Column(
            modifier = Modifier.weight(1.2f).verticalScroll(rememberScrollState())
        ) {
            QRInputAndOptions(viewModel, uiState, coinBalance, onDeductCoins, onShowInterstitialAd, showToast)
            if (showNativeAd) {
                Spacer(modifier = Modifier.height(16.dp))
                NativeAdViewComposable(nativeAd = nativeAd, showAd = true)
            }
        }
    }
}

// Shared UI component for input fields
@Composable
private fun QRInputAndOptions(
    viewModel: QRGenViewModel, uiState: QRGenUiState, coinBalance: Int,
    onDeductCoins: (Int) -> Boolean, onShowInterstitialAd: () -> Unit, showToast: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Create Your QR Code", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Current Coins: $coinBalance", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f))
        QRInputSection(
            textValue = uiState.inputText,
            onTextChanged = viewModel::onInputTextChanged,
            onGenerateClick = {
                onShowInterstitialAd()
                viewModel.generateQRCode(onDeductCoins, showToast)
            },
            currentErrorCorrectionLevel = uiState.errorCorrectionLevel
        )
        AdvancedOptions(
            selectedLevel = uiState.errorCorrectionLevel,
            onLevelSelected = viewModel::onErrorCorrectionLevelChanged
        )
    }
}

// --- Other UI components are restyled but call the same logic ---

@Composable
fun QRCodeDisplay(bitmap: Bitmap?, isGenerating: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isGenerating) {
                CircularProgressIndicator(strokeWidth = 4.dp, color = Color.White)
            } else if (bitmap != null) {
                // Add a white background specifically for the QR code image for scannability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Generated QR Code",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Filled.QrCode2,
                        contentDescription = "QR Code Placeholder",
                        modifier = Modifier.size(80.dp),
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Your QR Code appears here",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun QRInputSection(
    textValue: TextFieldValue,
    onTextChanged: (TextFieldValue) -> Unit,
    onGenerateClick: () -> Unit,
    currentErrorCorrectionLevel: ErrorCorrectionLevel
) {
    val generationCost = QRGenerator.getGenerationCost(currentErrorCorrectionLevel)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = textValue,
            onValueChange = onTextChanged,
            label = { Text("Enter text or URL", color = Color.White.copy(alpha = 0.7f)) },
            modifier = Modifier.fillMaxWidth(),
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
            onClick = onGenerateClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5856D6)
            )
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "Generate")
            Spacer(Modifier.width(8.dp))
            Text("GENERATE ($generationCost Coins)", fontWeight = FontWeight.Bold)
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
            value = "Correction: ${getErrorCorrectionLevelName(selectedLevel)}",
            onValueChange = {},
            readOnly = true,
            label = { Text("Advanced Options", color = Color.White.copy(alpha = 0.7f)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = "Settings", tint = Color.White) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.8f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            levels.forEach { level ->
                DropdownMenuItem(
                    text = { Text("${getErrorCorrectionLevelName(level)} (Cost: ${QRGenerator.getGenerationCost(level)})") },
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
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onSaveClick,
            enabled = isEnabled,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.15f),
                contentColor = Color.White,
                disabledContainerColor = Color.White.copy(alpha = 0.05f),
                disabledContentColor = Color.White.copy(alpha = 0.3f)
            )
        ) {
            Icon(Icons.Default.Save, contentDescription = "Save")
            Spacer(Modifier.width(8.dp))
            Text("Save")
        }
        Button(
            onClick = onShareClick,
            enabled = isEnabled,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.15f),
                contentColor = Color.White,
                disabledContainerColor = Color.White.copy(alpha = 0.05f),
                disabledContentColor = Color.White.copy(alpha = 0.3f)
            )
        ) {
            Icon(Icons.Default.Share, contentDescription = "Share")
            Spacer(Modifier.width(8.dp))
            Text("Share")
        }
    }
}
