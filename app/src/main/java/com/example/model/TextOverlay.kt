package com.example.model

import java.util.UUID

data class TextOverlay(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val color: Int = android.graphics.Color.WHITE,
    val fontSizeSp: Float = 24f,
    val xNormalized: Float = 0.5f,
    val yNormalized: Float = 0.5f
)
