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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

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

        val generationCost = 10
        if (!onDeductCoins(generationCost)) {
            showToast("Not enough coins! You need $generationCost coins to generate a QR code.")
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
    // Removed onShowRewardedAd from here as it's only for GainCoinsScreen
    showInlineAd: Boolean = false,
    showToast: (String) -> Unit // Callback for showing toasts
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "QR Code Generator",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Display current coin balance
            Text(
                text = "Current Coins: $coinBalance",
                style = MaterialTheme.typography.titleMedium,
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
                    viewModel.generateQRCode(onDeductCoins, showToast) // Pass coin logic
                    onShowInterstitialAd()
                }
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

            AnimatedVisibility(
                visible = showInlineAd && uiState.generatedBitmap != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                InlineBannerAd(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    isVisible = true
                )
            }
        }
    }
}

@Composable
fun QRCodeDisplay(bitmap: Bitmap?, isGenerating: Boolean) {
    Box(
        modifier = Modifier
            .size(280.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (isGenerating) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 6.dp)
        } else if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Generated QR Code",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.QrCode,
                    contentDescription = "QR Code Placeholder",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your QR Code will appear here",
                    style = MaterialTheme.typography.bodyLarge,
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
    onGenerateClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = textValue,
            onValueChange = onTextChanged,
            label = { Text("Enter text or URL to encode") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 5,
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = "Input") }
        )
        Button(
            onClick = onGenerateClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text("GENERATE QR CODE (10 Coins)", style = MaterialTheme.typography.titleMedium)
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
            label = { Text("Error Correction Level") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = "Settings") },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            levels.forEach { level ->
                DropdownMenuItem(
                    text = { Text(getErrorCorrectionLevelName(level)) },
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
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onSaveClick,
            enabled = isEnabled,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = "Save")
            Spacer(Modifier.width(8.dp))
            Text("Save")
        }

        Button(
            onClick = onShareClick,
            enabled = isEnabled,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = "Share")
            Spacer(Modifier.width(8.dp))
            Text("Share")
        }
    }
}
