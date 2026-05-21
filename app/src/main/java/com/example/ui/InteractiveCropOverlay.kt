package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private enum class DragHandle {
    NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
}

@Composable
fun InteractiveCropOverlay(
    cropLeft: Float,
    cropTop: Float,
    cropRight: Float,
    cropBottom: Float,
    onCropBoundsChanged: (left: Float, top: Float, right: Float, bottom: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val touchSlopPx = with(density) { 36.dp.toPx() } // Extra spacious hot-zone targeting smaller screen elements easily

    var activeHandle by remember { mutableStateOf(DragHandle.NONE) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var startLeft by remember { mutableStateOf(0f) }
    var startTop by remember { mutableStateOf(0f) }
    var startRight by remember { mutableStateOf(0f) }
    var startBottom by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()

                        val leftPx = cropLeft * w
                        val topPx = cropTop * h
                        val rightPx = cropRight * w
                        val bottomPx = cropBottom * h

                        // Check distance of start tap to each corner handle
                        val distTL = (offset - Offset(leftPx, topPx)).getDistance()
                        val distTR = (offset - Offset(rightPx, topPx)).getDistance()
                        val distBL = (offset - Offset(leftPx, bottomPx)).getDistance()
                        val distBR = (offset - Offset(rightPx, bottomPx)).getDistance()

                        activeHandle = when {
                            distTL < touchSlopPx -> DragHandle.TOP_LEFT
                            distTR < touchSlopPx -> DragHandle.TOP_RIGHT
                            distBL < touchSlopPx -> DragHandle.BOTTOM_LEFT
                            distBR < touchSlopPx -> DragHandle.BOTTOM_RIGHT
                            // If user is inside the crop rect but not on corners -> move center
                            offset.x in leftPx..rightPx && offset.y in topPx..bottomPx -> DragHandle.CENTER
                            else -> DragHandle.NONE
                        }

                        dragStartOffset = offset
                        startLeft = cropLeft
                        startTop = cropTop
                        startRight = cropRight
                        startBottom = cropBottom
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        if (w <= 0 || h <= 0 || activeHandle == DragHandle.NONE) return@detectDragGestures

                        // Normalize drag delta
                        val deltaX = dragAmount.x / w
                        val deltaY = dragAmount.y / h

                        val minSize = 0.15f // Keep a minimum 15% crop footprint to avoid sizing down to zero

                        when (activeHandle) {
                            DragHandle.TOP_LEFT -> {
                                val newLeft = (cropLeft + deltaX).coerceIn(0f, cropRight - minSize)
                                val newTop = (cropTop + deltaY).coerceIn(0f, cropBottom - minSize)
                                onCropBoundsChanged(newLeft, newTop, cropRight, cropBottom)
                            }
                            DragHandle.TOP_RIGHT -> {
                                val newRight = (cropRight + deltaX).coerceIn(cropLeft + minSize, 1f)
                                val newTop = (cropTop + deltaY).coerceIn(0f, cropBottom - minSize)
                                onCropBoundsChanged(cropLeft, newTop, newRight, cropBottom)
                            }
                            DragHandle.BOTTOM_LEFT -> {
                                val newLeft = (cropLeft + deltaX).coerceIn(0f, cropRight - minSize)
                                val newBottom = (cropBottom + deltaY).coerceIn(cropTop + minSize, 1f)
                                onCropBoundsChanged(newLeft, cropTop, cropRight, newBottom)
                            }
                            DragHandle.BOTTOM_RIGHT -> {
                                val newRight = (cropRight + deltaX).coerceIn(cropLeft + minSize, 1f)
                                val newBottom = (cropBottom + deltaY).coerceIn(cropTop + minSize, 1f)
                                onCropBoundsChanged(cropLeft, cropTop, newRight, newBottom)
                            }
                            DragHandle.CENTER -> {
                                val width = startRight - startLeft
                                val height = startBottom - startTop

                                // Total accumulative drag offset
                                val currentAccumulatedDeltaX = (change.position.x - dragStartOffset.x) / w
                                val currentAccumulatedDeltaY = (change.position.y - dragStartOffset.y) / h

                                val targetLeft = (startLeft + currentAccumulatedDeltaX).coerceIn(0f, 1f - width)
                                val targetTop = (startTop + currentAccumulatedDeltaY).coerceIn(0f, 1f - height)

                                onCropBoundsChanged(
                                    targetLeft,
                                    targetTop,
                                    targetLeft + width,
                                    targetTop + height
                                )
                            }
                            else -> {}
                        }
                    },
                    onDragEnd = {
                        activeHandle = DragHandle.NONE
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val leftPx = cropLeft * w
            val topPx = cropTop * h
            val rightPx = cropRight * w
            val bottomPx = cropBottom * h

            val dimColor = Color.Black.copy(alpha = 0.55f)

            // Draw 4 masking rectangles to dim space outside active crop region
            // 1. Top mask
            drawRect(color = dimColor, topLeft = Offset(0f, 0f), size = Size(w, topPx))
            // 2. Bottom mask
            drawRect(color = dimColor, topLeft = Offset(0f, bottomPx), size = Size(w, h - bottomPx))
            // 3. Left mask
            drawRect(color = dimColor, topLeft = Offset(0f, topPx), size = Size(leftPx, bottomPx - topPx))
            // 4. Right mask
            drawRect(color = dimColor, topLeft = Offset(rightPx, topPx), size = Size(w - rightPx, bottomPx - topPx))

            // Active rectangle stroke outline
            drawRect(
                color = Color.White,
                topLeft = Offset(leftPx, topPx),
                size = Size(rightPx - leftPx, bottomPx - topPx),
                style = Stroke(width = 2.dp.toPx())
            )

            // Core Rule of Thirds - gridlines (very stylish and helps map scenic shots)
            val subHorizontal = (bottomPx - topPx) / 3f
            val subVertical = (rightPx - leftPx) / 3f

            // horizontal lines
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(leftPx, topPx + subHorizontal),
                end = Offset(rightPx, topPx + subHorizontal),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
            )
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(leftPx, topPx + 2 * subHorizontal),
                end = Offset(rightPx, topPx + 2 * subHorizontal),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
            )

            // vertical lines
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(leftPx + subVertical, topPx),
                end = Offset(leftPx + subVertical, bottomPx),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
            )
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(leftPx + 2 * subVertical, topPx),
                end = Offset(leftPx + 2 * subVertical, bottomPx),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
            )

            // Heavy visual corner bracket anchors for modern interface accentuation
            val bracketLength = 18.dp.toPx()
            val bracketThickness = 4.dp.toPx()
            val bc = Color.White

            // Top-Left bracket
            drawLine(bc, Offset(leftPx, topPx), Offset(leftPx + bracketLength, topPx), bracketThickness)
            drawLine(bc, Offset(leftPx, topPx), Offset(leftPx, topPx + bracketLength), bracketThickness)

            // Top-Right bracket
            drawLine(bc, Offset(rightPx, topPx), Offset(rightPx - bracketLength, topPx), bracketThickness)
            drawLine(bc, Offset(rightPx, topPx), Offset(rightPx, topPx + bracketLength), bracketThickness)

            // Bottom-Left bracket
            drawLine(bc, Offset(leftPx, bottomPx), Offset(leftPx + bracketLength, bottomPx), bracketThickness)
            drawLine(bc, Offset(leftPx, bottomPx), Offset(leftPx, bottomPx - bracketLength), bracketThickness)

            // Bottom-Right bracket
            drawLine(bc, Offset(rightPx, bottomPx), Offset(rightPx - bracketLength, bottomPx), bracketThickness)
            drawLine(bc, Offset(rightPx, bottomPx), Offset(rightPx, bottomPx - bracketLength), bracketThickness)
        }
    }
}
