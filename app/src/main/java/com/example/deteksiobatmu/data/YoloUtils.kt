package com.example.deteksiobatmu.data

import android.graphics.RectF
import android.util.Log
import com.example.deteksiobatmu.domain.DetectionResult
import kotlin.math.max
import kotlin.math.min

fun processYOLOOutput(
    outputs: Array<FloatArray>,
    labels: List<String>,
    threshold: Float,
    iouThreshold: Float
): List<DetectionResult> {
    val results = mutableListOf<DetectionResult>()
    val imageSize = 640f

    for (output in outputs) {

        val objectness = output[4]

        // Cari skor kelas tertinggi
        val classScores = output.copyOfRange(5, output.size)
        val (classId, classScore) = classScores.withIndex().maxByOrNull { it.value } ?: continue

        //val score = output[4]
        //val classId = output[5].toInt()
        //if (score > threshold && classId in labels.indices) {
        val finalScore = objectness * classScore
        if (finalScore > threshold && classId in labels.indices) {
            val cx = output[0] * imageSize
            val cy = output[1] * imageSize
            val w = output[2] * imageSize
            val h = output[3] * imageSize
            val left = cx - w / 2
            val top = cy - h / 2

            Log.d("YOLOOutput", "class=$classId (${labels[classId]}), conf=$finalScore")

            results.add(
                DetectionResult(
                    label = labels[classId],
                    score = finalScore,
                    box = RectF(left, top, left + w, top + h)
                )
            )
        }

    }

    return nms(results, iouThreshold)
}

fun nms(detections: List<DetectionResult>, iouThreshold: Float): List<DetectionResult> {
    val selected = mutableListOf<DetectionResult>()
    val sorted = detections.sortedByDescending { it.score }

    val active = BooleanArray(sorted.size) { true }

    for (i in sorted.indices) {
        if (!active[i]) continue
        selected.add(sorted[i])

        for (j in (i + 1) until sorted.size) {
            if (iou(sorted[i].box, sorted[j].box) > iouThreshold) {
                active[j] = false
            }
        }
    }
    return selected
}

fun iou(a: RectF, b: RectF): Float {
    val intersection = RectF(
        max(a.left, b.left),
        max(a.top, b.top),
        min(a.right, b.right),
        min(a.bottom, b.bottom)
    )

    val interArea = max(0f, intersection.width()) * max(0f, intersection.height())
    val unionArea = a.width() * a.height() + b.width() * b.height() - interArea
    return if (unionArea == 0f) 0f else interArea / unionArea
}