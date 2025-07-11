package com.hulo.qrgenapp

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

private const val DEFAULT_QR_CODE_SIZE = 1024
private const val TAG = "QRCore"

data class QRCodeConfig(
    val content: String,
    val size: Int = DEFAULT_QR_CODE_SIZE,
    val foregroundColor: Int = Color.BLACK,
    val backgroundColor: Int = Color.WHITE,
    val errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.L
)

sealed class OperationResult {
    data class Success(val message: String) : OperationResult()
    data class Failure(val errorMessage: String) : OperationResult()
}

object QRGenerator {
    suspend fun generateQRCode(config: QRCodeConfig): Bitmap? {
        if (config.content.isBlank()) {
            println("$TAG: QR Code content cannot be empty.")
            return null
        }

        return withContext(Dispatchers.Default) {
            try {
                val hints = mapOf(
                    EncodeHintType.ERROR_CORRECTION to config.errorCorrectionLevel,
                    EncodeHintType.CHARACTER_SET to "UTF-8",
                )

                val writer = QRCodeWriter()

                val bitMatrix = writer.encode(
                    config.content,
                    BarcodeFormat.QR_CODE,
                    config.size,
                    config.size,
                    hints
                )

                val bitmap = Bitmap.createBitmap(config.size, config.size, Bitmap.Config.RGB_565)

                for (x in 0 until config.size) {
                    for (y in 0 until config.size) {
                        bitmap.setPixel(x, y, if (bitMatrix[x, y]) config.foregroundColor else config.backgroundColor)
                    }
                }
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun saveQRCodeToGallery(context: Context, bitmap: Bitmap, displayName: String): OperationResult {
        return withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            var imageUri: android.net.Uri? = null
            var outputStream: OutputStream? = null

            try {
                imageUri = resolver.insert(imageCollection, contentValues)
                    ?: throw Exception("MediaStore insert failed")

                outputStream = resolver.openOutputStream(imageUri)
                    ?: throw Exception("Failed to get output stream.")

                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "QR Code saved to Gallery", Toast.LENGTH_SHORT).show()
                }
                OperationResult.Success("Successfully saved to gallery.")

            } catch (e: Exception) {
                e.printStackTrace()
                imageUri?.let { resolver.delete(it, null, null) }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to save QR Code", Toast.LENGTH_SHORT).show()
                }
                OperationResult.Failure("Error saving image: ${e.message}")

            } finally {
                outputStream?.close()
            }
        }
    }
}

object QRDataProcessor {
    enum class QRContentType {
        TEXT,
        URL,
        EMAIL,
        PHONE,
        SMS,
        WIFI,
        VCARD,
        UNKNOWN
    }

    data class ProcessedQRData(
        val rawValue: String,
        val type: QRContentType,
        val processedValue: Any = rawValue
    )

    fun process(data: String): ProcessedQRData {
        return when {
            data.startsWith("http://") || data.startsWith("https://") ->
                ProcessedQRData(data, QRContentType.URL)
            data.startsWith("mailto:") ->
                ProcessedQRData(data, QRContentType.EMAIL, data.substringAfter("mailto:"))
            data.startsWith("tel:") ->
                ProcessedQRData(data, QRContentType.PHONE, data.substringAfter("tel:"))
            data.startsWith("smsto:") ->
                ProcessedQRData(data, QRContentType.SMS, data.substringAfter("smsto:"))
            data.startsWith("WIFI:") ->
                ProcessedQRData(data, QRContentType.WIFI)
            data.startsWith("BEGIN:VCARD") ->
                ProcessedQRData(data, QRContentType.VCARD)
            else -> ProcessedQRData(data, QRContentType.TEXT)
        }
    }
}

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun getErrorCorrectionLevels(): List<ErrorCorrectionLevel> {
    return ErrorCorrectionLevel.values().toList()
}

fun getErrorCorrectionLevelName(level: ErrorCorrectionLevel): String {
    return when (level) {
        ErrorCorrectionLevel.L -> "Low (~7%)"
        ErrorCorrectionLevel.M -> "Medium (~15%)"
        ErrorCorrectionLevel.Q -> "Quartile (~25%)"
        ErrorCorrectionLevel.H -> "High (~30%)"
    }
}
