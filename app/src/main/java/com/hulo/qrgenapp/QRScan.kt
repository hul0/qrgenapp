package com.hulo.qrgenapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Image
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
import android.util.Patterns
import androidx.camera.core.Preview as CameraPreviewUseCase

object QRDataProcessor {

    enum class QRContentType {
        URL,
        TEXT,
        WIFI,
        CONTACT_INFO,
        EMAIL,
        PHONE,
        SMS,
        GEO_LOCATION,
        CALENDAR_EVENT,
        APP_DEEP_LINK,
        UNKNOWN,
        BARCODE
    }

    data class ProcessedQRData(
        val rawValue: String,
        val type: QRContentType,
        val data: Map<String, String> = emptyMap()
    )

    fun process(rawValue: String): ProcessedQRData {
        if (rawValue.isBlank()) {
            return ProcessedQRData(rawValue, QRContentType.TEXT)
        }

        if (Patterns.WEB_URL.matcher(rawValue).matches() ||
            rawValue.startsWith("http://", ignoreCase = true) ||
            rawValue.startsWith("https://", ignoreCase = true) ||
            rawValue.matches(Regex("^[a-zA-Z0-9]+([-.][a-zA-Z0-9]+)*\\.[a-zA-Z]{2,}(:[0-9]{1,5})?(/.*)?$"))) {
            return ProcessedQRData(rawValue, QRContentType.URL)
        }

        if (rawValue.startsWith("WIFI:", ignoreCase = true)) {
            val params = parseKeyValueString(rawValue.substringAfter("WIFI:"), ';', ':')
            return ProcessedQRData(rawValue, QRContentType.WIFI, params)
        }

        if (rawValue.startsWith("BEGIN:VCARD", ignoreCase = true)) {
            val vcardData = mutableMapOf<String, String>()
            rawValue.lines().forEach { line ->
                if (line.contains(":")) {
                    val parts = line.split(":", limit = 2)
                    val key = parts[0].substringAfterLast(';').trim()
                    val value = parts[1].trim()
                    when {
                        key.equals("FN", ignoreCase = true) -> vcardData["fullName"] = value
                        key.equals("N", ignoreCase = true) -> {
                            val nameParts = value.split(';')
                            vcardData["lastName"] = nameParts.getOrElse(0) { "" }
                            vcardData["firstName"] = nameParts.getOrElse(1) { "" }
                        }
                        key.startsWith("TEL", ignoreCase = true) -> vcardData["phone"] = value
                        key.startsWith("EMAIL", ignoreCase = true) -> vcardData["email"] = value
                        key.startsWith("ORG", ignoreCase = true) -> vcardData["organization"] = value
                        key.startsWith("TITLE", ignoreCase = true) -> vcardData["title"] = value
                        key.startsWith("ADR", ignoreCase = true) -> vcardData["address"] = value.replace(";", ", ")
                        key.startsWith("URL", ignoreCase = true) -> vcardData["website"] = value
                    }
                }
            }
            return ProcessedQRData(rawValue, QRContentType.CONTACT_INFO, vcardData)
        }

        if (rawValue.startsWith("mailto:", ignoreCase = true)) {
            val emailData = mutableMapOf<String, String>()
            val mainPart = rawValue.substringAfter("mailto:")
            val parts = mainPart.split("?", limit = 2)
            emailData["to"] = parts[0]
            if (parts.size > 1) {
                val queryParams = parts[1].split('&')
                queryParams.forEach { param ->
                    val keyValue = param.split('=', limit = 2)
                    if (keyValue.size == 2) {
                        emailData[keyValue[0].lowercase()] = keyValue[1]
                    }
                }
            }
            return ProcessedQRData(rawValue, QRContentType.EMAIL, emailData)
        }
        if (Patterns.EMAIL_ADDRESS.matcher(rawValue).matches()) {
            return ProcessedQRData(rawValue, QRContentType.EMAIL, mapOf("to" to rawValue))
        }

        if (rawValue.startsWith("tel:", ignoreCase = true)) {
            return ProcessedQRData(rawValue, QRContentType.PHONE, mapOf("number" to rawValue.substringAfter("tel:")))
        }
        if (rawValue.matches(Regex("^\\+?[0-9\\s\\-()]{7,}$"))) {
            return ProcessedQRData(rawValue, QRContentType.PHONE, mapOf("number" to rawValue))
        }

        if (rawValue.startsWith("smsto:", ignoreCase = true) || rawValue.startsWith("sms:", ignoreCase = true)) {
            val smsData = mutableMapOf<String, String>()
            val prefix = if (rawValue.startsWith("smsto:", ignoreCase = true)) "smsto:" else "sms:"
            var content = rawValue.substringAfter(prefix)

            if (content.contains("?body=")) {
                val parts = content.split("?body=", limit = 2)
                smsData["number"] = parts[0]
                if (parts.size > 1) smsData["message"] = parts[1]
            } else {
                val parts = content.split(":", limit = 2)
                smsData["number"] = parts[0]
                if (parts.size > 1) smsData["message"] = parts[1]
            }
            return ProcessedQRData(rawValue, QRContentType.SMS, smsData)
        }

        if (rawValue.startsWith("geo:", ignoreCase = true)) {
            val geoData = mutableMapOf<String, String>()
            val content = rawValue.substringAfter("geo:")
            val parts = content.split("?q=", limit = 2)
            val latLng = parts[0].split(',')
            if (latLng.size == 2) {
                geoData["latitude"] = latLng[0]
                geoData["longitude"] = latLng[1]
            }
            if (parts.size > 1) {
                geoData["query"] = parts[1]
            }
            return ProcessedQRData(rawValue, QRContentType.GEO_LOCATION, geoData)
        }

        if (rawValue.startsWith("BEGIN:VEVENT", ignoreCase = true)) {
            val eventData = mutableMapOf<String, String>()
            rawValue.lines().forEach { line ->
                if (line.contains(":")) {
                    val (key, value) = line.split(":", limit = 2)
                    when {
                        key.equals("SUMMARY", ignoreCase = true) -> eventData["summary"] = value
                        key.equals("DTSTART", ignoreCase = true) -> eventData["startTime"] = value
                        key.equals("DTEND", ignoreCase = true) -> eventData["endTime"] = value
                        key.equals("LOCATION", ignoreCase = true) -> eventData["location"] = value
                        key.equals("DESCRIPTION", ignoreCase = true) -> eventData["description"] = value
                    }
                }
            }
            return ProcessedQRData(rawValue, QRContentType.CALENDAR_EVENT, eventData)
        }

        if (rawValue.startsWith("myapp://", ignoreCase = true)) {
            return ProcessedQRData(rawValue, QRContentType.APP_DEEP_LINK, mapOf("uri" to rawValue))
        }

        return ProcessedQRData(rawValue, QRContentType.TEXT)
    }

    private fun parseKeyValueString(
        input: String,
        entryDelimiter: Char,
        keyValueDelimiter: Char
    ): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val entries = input.split(entryDelimiter)
        for (entry in entries) {
            if (entry.isBlank()) continue
            val parts = entry.split(keyValueDelimiter, limit = 2)
            if (parts.size == 2) {
                map[parts[0].trim()] = parts[1].trim()
            } else if (parts.isNotEmpty()) {
                map[parts[0].trim()] = ""
            }
        }
        return map
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
    private val SCAN_COOLDOWN_MILLIS = 3 * 1000L

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = isGranted) }
    }

    fun onCodeScanned(result: QRDataProcessor.ProcessedQRData) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - _lastScanTime.value < SCAN_COOLDOWN_MILLIS) {
            _uiState.update { it.copy(toastMessage = "Scanning too fast! Please wait.") }
            return
        }

        _uiState.update {
            it.copy(
                scannedCode = result,
                isResultDialogShown = true,
                scanCount = it.scanCount + 1
            )
        }
        _lastScanTime.value = currentTime
    }

    fun dismissResultDialog() {
        _uiState.update {
            it.copy(
                isResultDialogShown = false
            )
        }
    }

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

    fun scanImage(uri: Uri, context: Context) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - _lastScanTime.value < SCAN_COOLDOWN_MILLIS) {
            _uiState.update { it.copy(toastMessage = "Scanning too fast! Please wait.") }
            return
        }

        try {
            val inputImage = InputImage.fromFilePath(context, uri)
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
            val scanner = BarcodeScanning.getClient(options)

            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        barcodes.first().rawValue?.let { rawValue ->
                            val processedData = QRDataProcessor.process(rawValue)
                            onCodeScanned(processedData)
                        }
                    } else {
                        _uiState.update { it.copy(toastMessage = "No QR code or barcode found in image") }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("QRScan", "Image scanning failed", e)
                    _uiState.update { it.copy(toastMessage = "Failed to scan image.") }
                }
        } catch (e: IOException) {
            Log.e("QRScan", "Failed to load image for scanning", e)
            _uiState.update { it.copy(toastMessage = "Could not load the selected image.") }
        } finally {
            _lastScanTime.value = currentTime
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

@Composable
fun QRScanScreen(
    viewModel: QRScanViewModel,
    onAddCoins: (Int) -> Unit,
    onAddScanToHistory: (String) -> Unit,
    onShowInterstitialAd: () -> Unit,
    isPremiumUser: Boolean,
    showToast: (String) -> Unit,
    nativeAd: NativeAd?,
    showNativeAd: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var isFlashlightOn by remember { mutableStateOf(false) }
    var hasFlashUnit by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                viewModel.scanImage(it, context)
            }
        }
    )

    uiState.toastMessage?.let { message ->
        LaunchedEffect(message) {
            context.showToast(message)
            viewModel.toastMessageShown()
        }
    }

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
                    context.showToast("No internet connection. Coins not awarded.")
                }
            }
            lastProcessedScanCount.value = uiState.scanCount
        }
    }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (uiState.hasCameraPermission) {
                CameraPreview(
                    isFlashlightOn = isFlashlightOn,
                    onHasFlashUnit = { hasFlashUnit = it },
                    onCodeScanned = { rawValue ->
                        val processedData = QRDataProcessor.process(rawValue)
                        viewModel.onCodeScanned(processedData)
                    }
                )
                ScannerOverlay()

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Import QR Code from Image",
                            tint = Color.White
                        )
                    }

                    if (hasFlashUnit) {
                        IconButton(
                            onClick = { isFlashlightOn = !isFlashlightOn },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isFlashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                contentDescription = "Toggle Flashlight",
                                tint = Color.White
                            )
                        }
                    }
                }

                if (uiState.coinCooldownRemaining > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Next coins in ${uiState.coinCooldownRemaining}s",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
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
            isPremiumUser = isPremiumUser,
            showToast = showToast
        )
    }
}


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

    LaunchedEffect(lifecycleOwner, cameraProviderFuture, previewView) {
        val cameraProvider = cameraProviderFuture.get()

        val previewUseCase = androidx.camera.core.Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val imageAnalysisUseCase = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, BarcodeAnalyzer(onCodeScanned))
            }

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                previewUseCase,
                imageAnalysisUseCase
            )
            onHasFlashUnit(camera?.cameraInfo?.hasFlashUnit() == true)
        } catch (e: Exception) {
            Log.e("QRScan", "CameraX binding failed", e)
        }
    }

    LaunchedEffect(isFlashlightOn, camera) {
        camera?.cameraControl?.enableTorch(isFlashlightOn)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            previewView
        },
        update = {
        }
    )
}


private class BarcodeAnalyzer(
    private val onCodeScanned: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: androidx.camera.core.ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        barcodes.first().rawValue?.let {
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
        } else {
            imageProxy.close()
        }
    }
}

@Composable
fun EnhancedScanResultDialog(
    result: QRDataProcessor.ProcessedQRData,
    onDismiss: () -> Unit,
    isPremiumUser: Boolean,
    showToast: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = "Info") },
        title = { Text("Code Scanned!") },
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
                            val intent = Intent(Intent.ACTION_VIEW, result.rawValue.toUri())
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
            text = "Point your camera at a QR code or barcode",
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
            text = "To scan QR codes and barcodes, this app requires camera permission. Please grant access to continue.",
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
