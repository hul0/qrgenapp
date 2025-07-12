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
import androidx.core.net.toUri

import android.util.Patterns // For URL and Email validation

object QRDataProcessor {

    enum class QRContentType {
        URL,
        TEXT,
        WIFI,
        CONTACT_INFO, // VCARD
        EMAIL,
        PHONE,
        SMS,
        GEO_LOCATION,
        CALENDAR_EVENT, // VEVENT
        APP_DEEP_LINK, // Custom scheme or known app schemes
        UNKNOWN
    }

    data class ProcessedQRData(
        val rawValue: String,
        val type: QRContentType,
        val data: Map<String, String> = emptyMap() // Optional: Store parsed data fields
    )

    fun process(rawValue: String): ProcessedQRData {
        if (rawValue.isBlank()) {
            return ProcessedQRData(rawValue, QRContentType.TEXT) // Or UNKNOWN if preferred
        }

        // --- URL ---
        // More robust URL check using Android's Patterns
        if (Patterns.WEB_URL.matcher(rawValue).matches() ||
            rawValue.startsWith("http://", ignoreCase = true) ||
            rawValue.startsWith("https://", ignoreCase = true) ||
            rawValue.matches(Regex("^[a-zA-Z0-9]+([-.][a-zA-Z0-9]+)*\\.[a-zA-Z]{2,}(:[0-9]{1,5})?(/.*)?$"))) { // Basic domain check
            return ProcessedQRData(rawValue, QRContentType.URL)
        }

        // --- WIFI ---
        // Format: WIFI:S:<SSID>;T:<WPA|WEP|nopass>;P:<PASSWORD>;H:<true|false|>;;
        if (rawValue.startsWith("WIFI:", ignoreCase = true)) {
            val params = parseKeyValueString(rawValue.substringAfter("WIFI:"), ';', ':')
            return ProcessedQRData(rawValue, QRContentType.WIFI, params)
        }

        // --- Contact Info (VCARD) ---
        // Simple check, VCARDs can be complex
        if (rawValue.startsWith("BEGIN:VCARD", ignoreCase = true)) {
            // Basic parsing, can be expanded significantly
            val vcardData = mutableMapOf<String, String>()
            rawValue.lines().forEach { line ->
                if (line.contains(":")) {
                    val parts = line.split(":", limit = 2)
                    val key = parts[0].substringAfterLast(';').trim() // Handle potential parameters like N;CHARSET=UTF-8
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

        // --- Email ---
        // mailto:someone@example.com?subject=Subject&body=BodyText
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
                        emailData[keyValue[0].lowercase()] = keyValue[1] // .URLDecoder.decode(keyValue[1], "UTF-8") if needed
                    }
                }
            }
            return ProcessedQRData(rawValue, QRContentType.EMAIL, emailData)
        }
        // Basic email regex as a fallback if not mailto:
        if (Patterns.EMAIL_ADDRESS.matcher(rawValue).matches()) {
            return ProcessedQRData(rawValue, QRContentType.EMAIL, mapOf("to" to rawValue))
        }

        // --- Phone Number ---
        // tel:<phone_number>
        if (rawValue.startsWith("tel:", ignoreCase = true)) {
            return ProcessedQRData(rawValue, QRContentType.PHONE, mapOf("number" to rawValue.substringAfter("tel:")))
        }
        // Basic phone number regex (very simplified, adapt for your needs)
        if (rawValue.matches(Regex("^\\+?[0-9\\s\\-()]{7,}$"))) {
            return ProcessedQRData(rawValue, QRContentType.PHONE, mapOf("number" to rawValue))
        }


        // --- SMS ---
        // smsto:<number>:<message> or sms:<number>?body=<message>
        if (rawValue.startsWith("smsto:", ignoreCase = true) || rawValue.startsWith("sms:", ignoreCase = true)) {
            val smsData = mutableMapOf<String, String>()
            val prefix = if (rawValue.startsWith("smsto:", ignoreCase = true)) "smsto:" else "sms:"
            var content = rawValue.substringAfter(prefix)

            if (content.contains("?body=")) { // For sms:number?body=message format
                val parts = content.split("?body=", limit = 2)
                smsData["number"] = parts[0]
                if (parts.size > 1) smsData["message"] = parts[1] // URLDecoder.decode(parts[1], "UTF-8") if needed
            } else { // For smsto:number:message format
                val parts = content.split(":", limit = 2)
                smsData["number"] = parts[0]
                if (parts.size > 1) smsData["message"] = parts[1]
            }
            return ProcessedQRData(rawValue, QRContentType.SMS, smsData)
        }

        // --- Geo Location ---
        // geo:<latitude>,<longitude>?q=<query>
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

        // --- Calendar Event (VEVENT) ---
        // Simple check, VEVENTs can be complex
        if (rawValue.startsWith("BEGIN:VEVENT", ignoreCase = true)) {
            // Basic parsing, can be expanded
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

        // --- App Deep Link (Example for a custom scheme) ---
        // myapp://screen/some_id
        if (rawValue.startsWith("myapp://", ignoreCase = true)) { // Replace "myapp" with your actual scheme
            return ProcessedQRData(rawValue, QRContentType.APP_DEEP_LINK, mapOf("uri" to rawValue))
        }

        // --- ISBN (International Standard Book Number) ---
        // Example: 978-3-16-148410-0
        // A simple regex might be too broad, but you could check for patterns like this
        // if (rawValue.matches(Regex("^(978|979)-[0-9]{1,5}-[0-9]{1,7}-[0-9]{1,6}-[0-9]$"))) {
        //     return ProcessedQRData(rawValue, QRContentType.ISBN, mapOf("isbn" to rawValue))
        // }
        // For simplicity, often TEXT is fine unless you have specific ISBN handling.

        // --- Default to TEXT ---
        // If none of the above, consider it plain text
        return ProcessedQRData(rawValue, QRContentType.TEXT)
    }

    /**
     * Helper function to parse key-value strings.
     * Example: "S:MySSID;T:WPA;P:MyPassword"
     * with entryDelimiter = ';', keyValueDelimiter = ':'
     * will produce mapOf("S" to "MySSID", "T" to "WPA", "P" to "MyPassword")
     */
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
                // Handle cases where a value might be empty after the delimiter (e.g. "KEY:;")
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
