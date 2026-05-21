package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import com.example.model.TextOverlay

enum class FilterType {
    NONE, GRAYSCALE, SEPIA, INVERT, VINTAGE, COOL, WARM, CONTRAST
}

object ImageUtils {

    // Scale the bitmap down if it exceeds max dimension to avoid OutOfMemory errors and speed up processing
    fun getResizedBitmap(bitmap: Bitmap, maxDimension: Int = 1200): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val (newWidth, newHeight) = if (width > height) {
            Pair(maxDimension, (maxDimension / ratio).toInt())
        } else {
            Pair((maxDimension * ratio).toInt(), maxDimension)
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun flipBitmap(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        val matrix = Matrix().apply {
            postScale(if (horizontal) -1f else 1f, if (vertical) -1f else 1f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun cropBitmap(bitmap: Bitmap, leftNorm: Float, topNorm: Float, rightNorm: Float, bottomNorm: Float): Bitmap {
        val left = (leftNorm * bitmap.width).toInt().coerceIn(0, bitmap.width - 2)
        val top = (topNorm * bitmap.height).toInt().coerceIn(0, bitmap.height - 2)
        val right = (rightNorm * bitmap.width).toInt().coerceIn(left + 2, bitmap.width)
        val bottom = (bottomNorm * bitmap.height).toInt().coerceIn(top + 2, bitmap.height)

        val width = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    // High performance inline matrix multiplication to combine filters and standard adjustments
    fun multiplyMatrices(a: FloatArray, b: FloatArray): FloatArray {
        val result = FloatArray(20)
        for (i in 0..3) { // Row of A
            for (j in 0..4) { // Column of B
                var value = 0f
                if (j < 4) {
                    for (k in 0..3) {
                        value += a[i * 5 + k] * b[k * 5 + j]
                    }
                } else {
                    for (k in 0..3) {
                        value += a[i * 5 + k] * b[k * 5 + 4]
                    }
                    value += a[i * 5 + 4] // Add A's translator
                }
                result[i * 5 + j] = value
            }
        }
        // Set standard alpha
        result[15] = 0f
        result[16] = 0f
        result[17] = 0f
        result[18] = 1f
        result[19] = 0f
        return result
    }

    // Single-pass combined adjustment matrix: Brightness + Contrast + Saturation
    // input brightness: -100f to 100f, contrast: 0.5f to 2.0f, saturation: 0f to 2.0f
    fun createAdjustmentMatrix(brightness: Float, contrast: Float, saturation: Float): FloatArray {
        val rWeight = 0.213f
        val gWeight = 0.715f
        val bWeight = 0.072f

        val s = saturation
        val c = contrast
        val b = brightness

        // Saturation matrix elements
        val ms0 = (1f - s) * rWeight + s
        val ms1 = (1f - s) * gWeight
        val ms2 = (1f - s) * bWeight

        val ms3 = (1f - s) * rWeight
        val ms4 = (1f - s) * gWeight + s
        val ms5 = (1f - s) * bWeight

        val ms6 = (1f - s) * rWeight
        val ms7 = (1f - s) * gWeight
        val ms8 = (1f - s) * bWeight + s

        // Combine with contrast scaling and brightness translation
        return floatArrayOf(
            ms0 * c, ms1 * c, ms2 * c, 0f, b,
            ms3 * c, ms4 * c, ms5 * c, 0f, b,
            ms6 * c, ms7 * c, ms8 * c, 0f, b,
            0f,      0f,      0f,      1f, 0f
        )
    }

    fun getFilterMatrix(filterType: FilterType): FloatArray {
        return when (filterType) {
            FilterType.NONE -> floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            FilterType.GRAYSCALE -> {
                val rw = 0.299f
                val gw = 0.587f
                val bw = 0.114f
                floatArrayOf(
                    rw, gw, bw, 0f, 0f,
                    rw, gw, bw, 0f, 0f,
                    rw, gw, bw, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            }
            FilterType.SEPIA -> floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f,     0f,     0f,     1f, 0f
            )
            FilterType.INVERT -> floatArrayOf(
                -1f,  0f,  0f, 0f, 255f,
                 0f, -1f,  0f, 0f, 255f,
                 0f,  0f, -1f, 0f, 255f,
                 0f,  0f,  0f, 1f,   0f
            )
            FilterType.VINTAGE -> floatArrayOf(
                0.90f, 0.05f, 0.05f, 0f, 15f,
                0.05f, 0.80f, 0.05f, 0f, 10f,
                0.02f, 0.05f, 0.65f, 0f, 5f,
                0f,    0f,    0f,    1f, 0f
            )
            FilterType.COOL -> floatArrayOf(
                0.80f, 0.05f, 0.05f, 0f, 0f,
                0.05f, 0.90f, 0.10f, 0f, 5f,
                0.02f, 0.10f, 1.25f, 0f, 15f,
                0f,    0f,    0f,    1f, 0f
            )
            FilterType.WARM -> floatArrayOf(
                1.20f, 0.05f, 0.02f, 0f, 15f,
                0.05f, 1.05f, 0.05f, 0f, 10f,
                0.02f, 0.02f, 0.80f, 0f, -5f,
                0f,    0f,    0f,    1f, 0f
            )
            FilterType.CONTRAST -> floatArrayOf(
                1.40f, 0f, 0f, 0f, -30f,
                0f, 1.40f, 0f, 0f, -30f,
                0f, 0f, 1.40f, 0f, -30f,
                0f, 0f, 0f, 1f, 0f
            )
        }
    }

    // Takes the original current working bitmap, applies filter + sliders, and burns texts onto layout
    fun renderFinalBitmap(
        baseBitmap: Bitmap,
        filterType: FilterType,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        texts: List<TextOverlay>
    ): Bitmap {
        val result = Bitmap.createBitmap(baseBitmap.width, baseBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Combine filter + adjustments
        val filterMat = getFilterMatrix(filterType)
        val adjustMat = createAdjustmentMatrix(brightness, contrast, saturation)
        val combinedMat = multiplyMatrices(adjustMat, filterMat)

        val cm = android.graphics.ColorMatrix(combinedMat)
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
        
        canvas.drawBitmap(baseBitmap, 0f, 0f, paint)

        // Text paints
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        for (textObj in texts) {
            // Scale text size relative to image width so that the result matches the visual scale in UI
            // Assuming visual width reference of 400dp
            val targetSizePx = textObj.fontSizeSp * (baseBitmap.width / 400f)

            textPaint.textSize = targetSizePx
            textPaint.color = textObj.color

            outlinePaint.textSize = targetSizePx
            outlinePaint.strokeWidth = targetSizePx * 0.15f // outline thicker for large text

            val xPx = textObj.xNormalized * baseBitmap.width
            val yPx = textObj.yNormalized * baseBitmap.height

            val textHeightOffset = (textPaint.descent() + textPaint.ascent()) / 2f
            val adjustedY = yPx - textHeightOffset

            // Draw shadow shadow outline, then draw main solid text
            canvas.drawText(textObj.text, xPx, adjustedY, outlinePaint)
            canvas.drawText(textObj.text, xPx, adjustedY, textPaint)
        }

        return result
    }

    // Dynamic generated bitmap for gorgeous offline fallback
    fun generateSunsetFallback(): Bitmap {
        val w = 1000
        val h = 1000
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw elegant linear sunset sky gradient
        val skyPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(0xFF1F1C2C.toInt(), 0xFF928DAB.toInt(), 0xFFE65C00.toInt(), 0xFFF9D423.toInt()),
                floatArrayOf(0.0f, 0.4f, 0.8f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), skyPaint)
        
        // Draw a giant glowy retro golden sun
        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFD700.toInt()
            alpha = 180
            shader = LinearGradient(
                0f, h * 0.4f, 0f, h * 0.8f,
                0xFFFF6B6B.toInt(), 0xFFF9D423.toInt(),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(w * 0.5f, h * 0.6f, w * 0.25f, sunPaint)

        // Draw geometric stylistic mountains
        val mountainPaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF4A154B.toInt()
            alpha = 230
        }
        val path1 = android.graphics.Path().apply {
            moveTo(0f, h.toFloat())
            lineTo(w * 0.35f, h * 0.65f)
            lineTo(w * 0.7f, h.toFloat())
            close()
        }
        canvas.drawPath(path1, mountainPaint1)

        val mountainPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF2A0845.toInt()
        }
        val path2 = android.graphics.Path().apply {
            moveTo(w * 0.4f, h.toFloat())
            lineTo(w * 0.75f, h * 0.55f)
            lineTo(w.toFloat(), h.toFloat())
            close()
        }
        canvas.drawPath(path2, mountainPaint2)

        return bitmap
    }
}
