package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object ReceiptImageUtils {
    private const val MAX_DIMENSION_PX = 960
    private const val MAX_BASE64_CHARS = 240_000

    suspend fun encodeReceiptUriToBase64(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            } ?: throw IllegalStateException("Unable to read selected image")

            var sampleSize = 1
            val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
            while (maxSide / sampleSize > MAX_DIMENSION_PX * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: throw IllegalStateException("Unable to decode receipt image")

            val scaled = if (maxOf(decoded.width, decoded.height) > MAX_DIMENSION_PX) {
                val ratio = MAX_DIMENSION_PX.toFloat() / maxOf(decoded.width, decoded.height).toFloat()
                val targetW = (decoded.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (decoded.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            try {
                var encoded = ""
                for (quality in listOf(75, 60, 45, 30)) {
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    val candidate = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                    encoded = candidate
                    if (candidate.length <= MAX_BASE64_CHARS) break
                }
                if (encoded.length > MAX_BASE64_CHARS) {
                    throw IllegalStateException("Receipt image is too large. Please select a smaller screenshot or crop it.")
                }
                encoded
            } finally {
                scaled.recycle()
            }
        }
    }

    fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}
