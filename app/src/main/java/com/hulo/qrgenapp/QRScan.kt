package com.hulo.qrgenapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.nativead.NativeAd
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import androidx.camera.core.Preview as CameraPreviewUseCase

data class QRScanUiState(
    val hasCameraPermission: Boolean = false,
    val scannedCode: QRDataProcessor.ProcessedQRData? = null,
    val isResultDialogShown: Boolean = false,
    val scanCount: Int = 0,
    val scanCooldownRemaining: Int = 0 // New state for scan cooldown
)

class QRScanViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(QRScanUiState())
    val uiState: StateFlow<QRScanUiState> = _uiState.asStateFlow()

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = isGranted) }
    }

    fun onCodeScanned(result: QRDataProcessor.ProcessedQRData) {
        _uiState.update {
            it.copy(
                scannedCode = result,
                isResultDialogShown = true,
                scanCount = it.scanCount + 1
            )
        }
    }

    fun dismissResultDialog() {
        _uiState.update {
            it.copy(
                isResultDialogShown = false
            )
        }
    }

    // Function to start the scan cooldown
    fun startScanCooldown() {
        // Only start if not already in cooldown or if cooldown just finished (i.e., it's 0)
        if (_uiState.value.scanCooldownRemaining == 0) {
            _uiState.update { it.copy(scanCooldownRemaining = 10) } // 10 seconds cooldown
            viewModelScope.launch {
                for (i in 9 downTo 0) {
                    delay(1000L)
                    _uiState.update { it.copy(scanCooldownRemaining = i) }
                }
                // After cooldown, ensure it's explicitly 0
                _uiState.update { it.copy(scanCooldownRemaining = 0) }
            }
        }
    }
}

@Composable
fun QRScanScreen(
    viewModel: QRScanViewModel,
    onAddCoins: (Int) -> Unit, // Callback to add coins
    onAddScanToHistory: (String) -> Unit, // New: Callback to add scan to history
    onShowInterstitialAd: () -> Unit,
    isPremiumUser: Boolean, // New: Premium status
    showToast: (String) -> Unit, // Callback for showing toasts
    nativeAd: NativeAd?, // New: Native Ad
    showNativeAd: Boolean // New: Control native ad visibility
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.onPermissionResult(isGranted)
            if (!isGranted) {
                context.showToast("Camera permission is required to scan QR codes.")
            }
        }
    )

    LaunchedEffect(key1 = true) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScanStatsCard(
            scanCount = uiState.scanCount,
            isPremiumUser = isPremiumUser // Pass premium status
        )

        // Native Ad Section
        NativeAdViewComposable(
            nativeAd = nativeAd,
            showAd = showNativeAd
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (uiState.hasCameraPermission) {
                CameraPreview(
                    canScan = uiState.scanCooldownRemaining == 0, // Pass canScan state
                    onCodeScanned = { rawValue ->
                        val processedData = QRDataProcessor.process(rawValue)
                        viewModel.onCodeScanned(processedData)
                        onAddScanToHistory(rawValue) // Add to history
                        // Check for internet connection before adding coins
                        if (isNetworkAvailable(context)) {
                            onAddCoins(5) // Add 5 coins for scanning
                            if (!isPremiumUser) { // Only show interstitial if not premium
                                onShowInterstitialAd() // Show interstitial ad after scan
                            }
                        } else {
                            context.showToast("No internet connection. Coins not awarded.")
                        }
                    },
                    onScanInitiated = {
                        viewModel.startScanCooldown() // Start cooldown when a scan is detected
                    }
                )
                ScannerOverlay()

                // Display countdown if cooldown is active
                if (uiState.scanCooldownRemaining > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scanning Cooldown",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Scanning Cooldown",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Next scan in ${uiState.scanCooldownRemaining} seconds...",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                CircularProgressIndicator(
                                    progress = uiState.scanCooldownRemaining / 10f,
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }

            } else {
                PermissionDeniedScreen {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }

    if (uiState.isResultDialogShown && uiState.scannedCode != null) {
        EnhancedScanResultDialog(
            result = uiState.scannedCode!!,
            onDismiss = { viewModel.dismissResultDialog() },
            isPremiumUser = isPremiumUser, // Pass premium status
            showToast = showToast
        )
    }
}

@Composable
fun ScanStatsCard(
    scanCount: Int,
    isPremiumUser: Boolean // New parameter
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Scans Today",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = scanCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Display premium status or prompt to go premium
            if (isPremiumUser) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Premium User",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Premium User",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            } else {
                Button(
                    onClick = { /* This button can navigate to PremiumPlanScreen if needed */ },
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Go Premium",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Go Premium", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    canScan: Boolean, // New parameter to control scanning
    onCodeScanned: (String) -> Unit,
    onScanInitiated: () -> Unit // New callback for when a scan is detected
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProvider = cameraProviderFuture.get()

            val preview = CameraPreviewUseCase.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    // Pass canScan and onScanInitiated to the analyzer
                    it.setAnalyzer(cameraExecutor, QRCodeAnalyzer(canScan, onCodeScanned, onScanInitiated))
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("QRScan", "CameraX binding failed", e)
            }

            previewView
        },
        update = { previewView ->
            // Update the analyzer with the latest canScan state
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll() // Unbind to rebind with updated analyzer
            val preview = CameraPreviewUseCase.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QRCodeAnalyzer(canScan, onCodeScanned, onScanInitiated))
                }
            try {
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("QRScan", "CameraX binding failed during update", e)
            }
        }
    )
}

private class QRCodeAnalyzer(
    private val canScan: Boolean, // New parameter to control scanning
    private val onCodeScanned: (String) -> Unit,
    private val onScanInitiated: () -> Unit // New callback
) : ImageAnalysis.Analyzer {
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: androidx.camera.core.ImageProxy) {
        if (!canScan) { // Check canScan before processing
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        barcodes.first().rawValue?.let {
                            onScanInitiated() // Notify ViewModel to start cooldown
                            onCodeScanned(it)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("QRScan", "ML Kit scanning failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
}

@Composable
fun EnhancedScanResultDialog(
    result: QRDataProcessor.ProcessedQRData,
    onDismiss: () -> Unit,
    isPremiumUser: Boolean, // New: Premium status
    showToast: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = "Info") },
        title = { Text("QR Code Scanned!") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Type: ${result.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                SelectionContainer {
                    Text(
                        result.rawValue,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isPremiumUser) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Premium Tip",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Premium Tip",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Your scan history is limited. Go Premium for unlimited history!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(result.rawValue))
                    context.showToast("Copied to clipboard")
                    onDismiss()
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Copy")
            }
        },
        dismissButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (result.type == QRDataProcessor.QRContentType.URL) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.rawValue))
                            context.startActivity(intent)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = "Open Link", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open Link")
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

@Composable
fun ScannerOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(Color.Transparent)
                .border(4.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
        )
        Text(
            text = "Point your camera at a QR code",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun PermissionDeniedScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Permission Denied",
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Camera Access Needed",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "To scan QR codes, this app requires camera permission. Please grant access to continue.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("Grant Camera Permission", style = MaterialTheme.typography.titleMedium)
        }
    }
}

// Function to check network availability
fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

fun android.content.Context.showToast(message: String) {
    android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
}
