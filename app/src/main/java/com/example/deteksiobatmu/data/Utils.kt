package com.example.deteksiobatmu.data

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import android.graphics.BitmapFactory
//import android.util.Half


fun ImageProxy.toBitmap(): Bitmap? {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}


//fun floatToHalf(value: Float): Short {
//    val intBits = java.lang.Float.floatToIntBits(value)
//    val sign = (intBits ushr 16) and 0x8000
//    var exponent = ((intBits ushr 23) and 0xFF) - 127 + 15
//    var mantissa = (intBits ushr 13) and 0x3FF
//
//    if (exponent <= 0) {
//        // Denormalized number or underflow
//        if (exponent < -10) {
//            return sign.toShort()
//        }
//        mantissa = (mantissa or 0x400) shr (1 - exponent)
//        exponent = 0
//    } else if (exponent >= 31) {
//        // Overflow to infinity
//        return (sign or 0x7C00).toShort()
//    }
//
//    return (sign or (exponent shl 10) or mantissa).toShort()
//}


fun Bitmap.centerCrop(targetWidth: Int, targetHeight: Int): Bitmap {
    val scale = maxOf(
        targetWidth.toFloat() / width,
        targetHeight.toFloat() / height
    )
    val scaledWidth = (scale * width).toInt()
    val scaledHeight = (scale * height).toInt()

    val scaledBitmap = Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)

    val xOffset = (scaledWidth - targetWidth) / 2
    val yOffset = (scaledHeight - targetHeight) / 2

    return Bitmap.createBitmap(scaledBitmap, xOffset, yOffset, targetWidth, targetHeight)
}