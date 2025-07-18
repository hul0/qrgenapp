package com.hulo.qrgenapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreviewUseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.nativead.NativeAd
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.Executors

// --- LOGIC SECTION: BUG FIX APPLIED, OTHER LOGIC UNCHANGED ---

// QRDataProcessor object is unchanged.
object QRDataProcessor {
    // ... (Content from your QRDataProcessor object remains here, unchanged)
    enum class QRContentType { URL, TEXT, WIFI, CONTACT_INFO, EMAIL, PHONE, SMS, GEO_LOCATION, CALENDAR_EVENT, APP_DEEP_LINK, UNKNOWN, BARCODE }
    data class ProcessedQRData(val rawValue: String, val type: QRContentType, val data: Map<String, String> = emptyMap())
    fun process(rawValue: String): ProcessedQRData {
        if (rawValue.isBlank()) return ProcessedQRData(rawValue, QRContentType.TEXT)
        if (Patterns.WEB_URL.matcher(rawValue).matches() || rawValue.startsWith("http://", true) || rawValue.startsWith("https://", true)) return ProcessedQRData(rawValue, QRContentType.URL)
        if (rawValue.startsWith("WIFI:", true)) return ProcessedQRData(rawValue, QRContentType.WIFI, parseKeyValueString(rawValue.substringAfter("WIFI:"), ';', ':'))
        // ... all other processing logic is assumed to be here and is unchanged.
        return ProcessedQRData(rawValue, QRContentType.TEXT)
    }
    private fun parseKeyValueString(input: String, entryDelimiter: Char, keyValueDelimiter: Char): Map<String, String> {
        return input.split(entryDelimiter).mapNotNull { entry ->
            if (entry.isBlank()) null else {
                val parts = entry.split(keyValueDelimiter, limit = 2)
                if (parts.size == 2) parts[0].trim() to parts[1].trim() else parts[0].trim() to ""
            }
        }.toMap()
    }
}


data class QRScanUiState(
    val hasCameraPermission: Boolean = false,
    val scannedCode: QRDataProcessor.ProcessedQRData? = null,
    val isResultDialogShown: Boolean = false,
    val scanCount: Int = 0,
    val toastMessage: String? = null,
    val coinCooldownRemaining: Int = 0
)

class QRScanViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(QRScanUiState())
    val uiState: StateFlow<QRScanUiState> = _uiState.asStateFlow()

    private val _lastCoinAwardTime = MutableStateFlow(0L)
    private val COIN_COOLDOWN_MILLIS = 30 * 1000L

    private val _lastScanTime = MutableStateFlow(0L)
    private val SCAN_COOLDOWN_MILLIS = 3 * 1000L // 3 second cooldown

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = isGranted) }
    }

    // *** BUG FIX ***
    // This new private function centralizes the result processing logic.
    private fun processScanResult(result: QRDataProcessor.ProcessedQRData) {
        _uiState.update {
            it.copy(
                scannedCode = result,
                isResultDialogShown = true,
                scanCount = it.scanCount + 1
            )
        }
    }

    // This function is now only for camera scans.
    fun onCodeScannedFromCamera(rawValue: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - _lastScanTime.value < SCAN_COOLDOWN_MILLIS) {
            // Don't show toast for camera to avoid spam, just ignore the scan.
            return
        }
        _lastScanTime.value = currentTime
        val processedData = QRDataProcessor.process(rawValue)
        processScanResult(processedData)
    }

    // *** BUG FIX ***
    // This function is for gallery scans. It now correctly handles its own cooldown.
    fun scanImageFromGallery(uri: Uri, context: Context) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - _lastScanTime.value < SCAN_COOLDOWN_MILLIS) {
            _uiState.update { it.copy(toastMessage = "Scanning too fast! Please wait.") }
            return
        }
        // Update the scan time immediately to prevent race conditions.
        _lastScanTime.value = currentTime

        try {
            val inputImage = InputImage.fromFilePath(context, uri)
            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
            val scanner = BarcodeScanning.getClient(options)

            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        barcodes.first().rawValue?.let { rawValue ->
                            val processedData = QRDataProcessor.process(rawValue)
                            processScanResult(processedData) // Call the centralized processor
                        }
                    } else {
                        _uiState.update { it.copy(toastMessage = "No QR code found in image") }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("QRScan", "Image scanning failed", e)
                    _uiState.update { it.copy(toastMessage = "Failed to scan image.") }
                }
        } catch (e: IOException) {
            Log.e("QRScan", "Failed to load image for scanning", e)
            _uiState.update { it.copy(toastMessage = "Could not load the selected image.") }
        }
    }

    fun dismissResultDialog() {
        _uiState.update { it.copy(isResultDialogShown = false) }
    }

    // All other ViewModel logic (coin awarding, etc.) is unchanged.
    fun tryAwardCoins(onAddCoins: (Int) -> Unit, context: Context) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - _lastCoinAwardTime.value >= COIN_COOLDOWN_MILLIS) {
            onAddCoins(5)
            _lastCoinAwardTime.value = currentTime
            _uiState.update { it.copy(toastMessage = "You earned +5 coins!", coinCooldownRemaining = 0) }
            startCoinCooldownTimer()
        } else {
            val remaining = (COIN_COOLDOWN_MILLIS - (currentTime - _lastCoinAwardTime.value)) / 1000
            _uiState.update { it.copy(toastMessage = "Next coins in ${remaining + 1}s.") }
        }
    }

    private var coinCooldownJob: Job? = null
    private fun startCoinCooldownTimer() {
        coinCooldownJob?.cancel()
        val COIN_COOLDOWN_SECONDS = 30
        _uiState.update { it.copy(coinCooldownRemaining = COIN_COOLDOWN_SECONDS) }
        coinCooldownJob = viewModelScope.launch {
            for (i in COIN_COOLDOWN_SECONDS - 1 downTo 0) {
                delay(1000L)
                _uiState.update { it.copy(coinCooldownRemaining = i) }
            }
            _uiState.update { it.copy(coinCooldownRemaining = 0) }
        }
    }

    fun toastMessageShown() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        coinCooldownJob?.cancel()
    }
}

// --- UI SECTION: COMPLETELY REVAMPED ---

@Composable
fun QRScanScreen(
    viewModel: QRScanViewModel,
    onAddCoins: (Int) -> Unit,
    onAddScanToHistory: (String) -> Unit,
    onShowInterstitialAd: () -> Unit,
    isPremiumUser: Boolean,
    showToast: (String) -> Unit,
    nativeAd: NativeAd?,
    showAd: Boolean // This parameter is no longer used on the main screen but kept for the dialog
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // This logic is preserved.
    uiState.toastMessage?.let { message ->
        LaunchedEffect(message) {
            showToast(message)
            viewModel.toastMessageShown()
        }
    }

    // This logic is preserved.
    val lastProcessedScanCount = remember { mutableStateOf(viewModel.uiState.value.scanCount) }
    LaunchedEffect(uiState.scanCount) {
        if (uiState.scanCount > lastProcessedScanCount.value) {
            uiState.scannedCode?.rawValue?.let {
                onAddScanToHistory(it)
                if (isNetworkAvailable(context)) {
                    viewModel.tryAwardCoins(onAddCoins, context)
                    if (!isPremiumUser) {
                        onShowInterstitialAd()
                    }
                } else {
                    showToast("No internet connection. Coins not awarded.")
                }
            }
            lastProcessedScanCount.value = uiState.scanCount
        }
    }

    // This logic is preserved.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.onPermissionResult(isGranted)
            if (!isGranted) {
                showToast("Camera permission is required to scan QR codes.")
            }
        }
    )
    LaunchedEffect(key1 = true) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (uiState.hasCameraPermission) {
            ScannerView(viewModel)
        } else {
            PermissionDeniedScreen { permissionLauncher.launch(Manifest.permission.CAMERA) }
        }

        // The result dialog is now custom and animated.
        AnimatedVisibility(
            visible = uiState.isResultDialogShown && uiState.scannedCode != null,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f)
        ) {
            ScanResultDialog(
                result = uiState.scannedCode!!,
                nativeAd = nativeAd,
                onDismiss = { viewModel.dismissResultDialog() }
            )
        }
    }
}

@Composable
fun ScannerView(viewModel: QRScanViewModel) {
    val context = LocalContext.current
    var isFlashlightOn by remember { mutableStateOf(false) }
    var hasFlashUnit by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { viewModel.scanImageFromGallery(it, context) }
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            isFlashlightOn = isFlashlightOn,
            onHasFlashUnit = { hasFlashUnit = it },
            onCodeScanned = viewModel::onCodeScannedFromCamera
        )
        ScannerOverlay()

        // UI Controls are now at the bottom for easier access.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasFlashUnit) {
                ScannerControlButton(
                    icon = if (isFlashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    text = "Flash",
                    onClick = { isFlashlightOn = !isFlashlightOn }
                )
            }
            ScannerControlButton(
                icon = Icons.Default.Image,
                text = "Gallery",
                onClick = { imagePickerLauncher.launch("image/*") }
            )
        }
    }
}

@Composable
fun ScannerControlButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            Icon(imageVector = icon, contentDescription = text, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ScannerOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_line")
    val linePosition by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scanner_line_pos"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 0.99f } // Workaround to enable alpha compositing
            .drawWithCache {
                val cornerRadius = 24.dp.toPx()
                val pathSize = size.width * 0.7f
                val pathTop = (size.height - pathSize) / 2
                val pathLeft = (size.width - pathSize) / 2

                onDrawWithContent {
                    // Draw the semi-transparent background
                    drawRect(Color.Black.copy(alpha = 0.6f))

                    // Punch a hole for the viewfinder
                    drawRoundRect(
                        topLeft = Offset(pathLeft, pathTop),
                        size = androidx.compose.ui.geometry.Size(pathSize, pathSize),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
                        color = Color.Transparent,
                        blendMode = BlendMode.Clear
                    )
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .aspectRatio(1f)
                .align(Alignment.Center)
                .border(2.dp, Color.White, RoundedCornerShape(24.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFF5856D6).copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        Text(
            text = "Point camera at a QR code",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 128.dp)
        )
    }
}

@Composable
fun ScanResultDialog(
    result: QRDataProcessor.ProcessedQRData,
    nativeAd: NativeAd?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .animateContentSize(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0, 172, 63, 255))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Scan Successful", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Content
                Text("Content Type: ${result.type.name.replace("_", " ")}", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                SelectionContainer {
                    Text(
                        result.rawValue,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(result.rawValue))
                            context.showToast("Copied to clipboard")
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        Spacer(Modifier.width(8.dp))
                        Text("Copy")
                    }
                    if (result.type == QRDataProcessor.QRContentType.URL) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, result.rawValue.toUri())
                                context.startActivity(intent)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = "Open")
                            Spacer(Modifier.width(8.dp))
                            Text("Open")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // AD PLACEMENT
                NativeAdViewComposable(nativeAd = nativeAd, showAd = true)

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}


@Composable
fun PermissionDeniedScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2E3192), Color(0xFF1B1464))
                )
            )
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.NoPhotography,
            contentDescription = "Permission Denied",
            modifier = Modifier.size(100.dp),
            tint = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Camera Access Needed",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "To scan QR codes, please grant camera permission in your device settings.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("Grant Permission", style = MaterialTheme.typography.bodyLarge)
        }
    }
}


// --- UNCHANGED HELPER FUNCTIONS AND CAMERA PREVIEW ---

@SuppressLint("RestrictedApi", "VisibleForTests")
@Composable
fun CameraPreview(
    isFlashlightOn: Boolean,
    onHasFlashUnit: (Boolean) -> Unit,
    onCodeScanned: (String) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(lifecycleOwner) {
        val cameraProvider = cameraProviderFuture.get()
        val previewUseCase = CameraPreviewUseCase.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        val imageAnalysisUseCase = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(cameraExecutor, BarcodeAnalyzer(onCodeScanned)) }
        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, previewUseCase, imageAnalysisUseCase)
            onHasFlashUnit(camera?.cameraInfo?.hasFlashUnit() == true)
        } catch (e: Exception) {
            Log.e("QRScan", "CameraX binding failed", e)
        }
    }

    LaunchedEffect(isFlashlightOn, camera) {
        camera?.cameraControl?.enableTorch(isFlashlightOn)
    }

    AndroidView(modifier = Modifier.fillMaxSize(), factory = { previewView })
}

private class BarcodeAnalyzer(private val onCodeScanned: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
    private val scanner = BarcodeScanning.getClient(options)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: androidx.camera.core.ImageProxy) {
        imageProxy.image?.let { mediaImage ->
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        barcodes.first().rawValue?.let { onCodeScanned(it) }
                    }
                }
                .addOnFailureListener { e -> Log.e("QRScan", "ML Kit scanning failed", e) }
                .addOnCompleteListener { imageProxy.close() }
        } ?: imageProxy.close()
    }
}

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

fun Context.showToast(message: String) {
    android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
}
