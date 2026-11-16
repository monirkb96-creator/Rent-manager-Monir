package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object ReceiptImageUtils {
    private const val MAX_DIMENSION_PX = 960
    private const val FALLBACK_DIMENSION_PX = 640
    private const val TINY_DIMENSION_PX = 480
    private const val MAX_BASE64_CHARS = 210_000

    suspend fun encodeReceiptUriToBase64(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val decoded = decodeScaledBitmap(context, uri, MAX_DIMENSION_PX)
                ?: throw IllegalStateException("Unable to read selected image")

            var currentBitmap = decoded
            try {
                var encoded = compressWithinLimit(currentBitmap)
                if (encoded.length > MAX_BASE64_CHARS) {
                    val smaller = downscaleBitmap(currentBitmap, FALLBACK_DIMENSION_PX)
                    if (smaller !== currentBitmap) {
                        currentBitmap.recycle()
                        currentBitmap = smaller
                    }
                    encoded = compressWithinLimit(currentBitmap)
                }
                if (encoded.length > MAX_BASE64_CHARS) {
                    val tiny = downscaleBitmap(currentBitmap, TINY_DIMENSION_PX)
                    if (tiny !== currentBitmap) {
                        currentBitmap.recycle()
                        currentBitmap = tiny
                    }
                    encoded = compressWithinLimit(currentBitmap)
                }
                if (encoded.length > MAX_BASE64_CHARS) {
                    throw IllegalStateException("Receipt image is too large. Please select a smaller photo or crop it.")
                }
                encoded
            } finally {
                if (!currentBitmap.isRecycled) {
                    currentBitmap.recycle()
                }
            }
        }
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri, maxDimPx: Int): Bitmap? {
        val resolver = context.contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(resolver, uri)
                return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val width = info.size.width
                    val height = info.size.height
                    val longest = maxOf(width, height)
                    if (longest > maxDimPx && width > 0 && height > 0) {
                        val ratio = maxDimPx.toFloat() / longest.toFloat()
                        decoder.setTargetSize(
                            (width * ratio).toInt().coerceAtLeast(1),
                            (height * ratio).toInt().coerceAtLeast(1)
                        )
                    }
                }
            } catch (_: Exception) {
                // Fall through to BitmapFactory fallback
            }
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            val firstStream = resolver.openInputStream(uri)
                ?: resolver.openAssetFileDescriptor(uri, "r")?.createInputStream()
            firstStream?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
        } catch (_: Exception) {
            // Best-effort bounds check; fall through with sampleSize = 1 or 2
        }

        var sampleSize = 1
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        if (maxSide > 0) {
            while (maxSide / sampleSize > maxDimPx * 2) {
                sampleSize *= 2
            }
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = try {
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        } catch (_: Exception) {
            null
        } ?: try {
            resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor, null, decodeOpts)
            }
        } catch (_: Exception) {
            null
        } ?: try {
            resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                BitmapFactory.decodeFileDescriptor(afd.fileDescriptor, null, decodeOpts)
            }
        } catch (_: Exception) {
            null
        } ?: return null

        return downscaleBitmap(decoded, maxDimPx).also { scaled ->
            if (scaled !== decoded) decoded.recycle()
        }
    }

    private fun downscaleBitmap(bitmap: Bitmap, maxDimPx: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxDimPx) return bitmap
        val ratio = maxDimPx.toFloat() / longest.toFloat()
        val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun compressWithinLimit(bitmap: Bitmap): String {
        var encoded = ""
        for (quality in listOf(80, 65, 50, 35, 25)) {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val candidate = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            encoded = candidate
            if (candidate.length <= MAX_BASE64_CHARS) break
        }
        return encoded
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
