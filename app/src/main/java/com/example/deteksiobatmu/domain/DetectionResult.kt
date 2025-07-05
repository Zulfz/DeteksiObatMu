package com.example.deteksiobatmu.domain

import android.graphics.RectF

class DetectionResult (
    val label: String,
    val score: Float,
    val box: RectF
)