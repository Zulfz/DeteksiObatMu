package com.example.deteksiobatmu.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.deteksiobatmu.data.centerCrop
//import com.example.deteksiobatmu.data.floatToHalf
import com.example.deteksiobatmu.data.processYOLOOutput
import com.example.deteksiobatmu.domain.DetectionResult
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.util.Half


//import centerCrop



class MedicineImageAnalyzer (
    private val context: Context,
    private val interpreter: Interpreter,
    private val labels: List<String>,
    private val onResults: (List<DetectionResult>) -> Unit
) : ImageAnalysis.Analyzer {

    private var frameSkipCounter = 0

    override fun analyze(imageProxy: ImageProxy) {
        if (frameSkipCounter % 30 == 0) {
            val bitmap = imageProxy.toBitmap().centerCrop(640, 640) ?: return
            val input = preprocess(bitmap)

            //val output = Array(1) { Array(8400) { FloatArray(6) } } // For YOLOv8
            val output = Array(1) { Array(14) { FloatArray(8400) } } // Match model output shape
            interpreter.run(input, output)
            Log.d("ModelOutput", "Shape: ${output.size} x ${output[0].size} x ${output[0][0].size}")


            val transposed = Array(8400) { FloatArray(14) }
            for (i in 0 until 14) {
                for (j in 0 until 8400) {
                     transposed[j][i] = output[0][i][j]
                }
            }




            val results = processYOLOOutput(transposed, labels, threshold = 0.2f, iouThreshold = 0.45f)
            onResults(results)
        }
        frameSkipCounter++
        imageProxy.close()
    }



    private fun preprocess(bitmap: android.graphics.Bitmap): java.nio.ByteBuffer {
        val imgSize = 640
        val buffer = java.nio.ByteBuffer.allocateDirect(4 * imgSize * imgSize * 3)
        buffer.order(java.nio.ByteOrder.nativeOrder())
        val resized = android.graphics.Bitmap.createScaledBitmap(bitmap, imgSize, imgSize, true)

        for (y in 0 until imgSize) {
            for (x in 0 until imgSize) {
                val px = resized.getPixel(x, y)
                buffer.putFloat((android.graphics.Color.red(px) / 255f))
                buffer.putFloat((android.graphics.Color.green(px) / 255f))
                buffer.putFloat((android.graphics.Color.blue(px) / 255f))
            }
        }
        return buffer
    }
}


//    private fun preprocess(bitmap: Bitmap): ByteBuffer {
//        val imgSize = 640
//        val buffer = ByteBuffer.allocateDirect(2 * imgSize * imgSize * 3)
//        buffer.order(ByteOrder.nativeOrder())
//        val resized = Bitmap.createScaledBitmap(bitmap, imgSize, imgSize, true)
//
//        for (y in 0 until imgSize) {
//            for (x in 0 until imgSize) {
//                val px = resized.getPixel(x, y)
//                buffer.putShort(floatToHalf(Color.red(px) / 255f))
//                buffer.putShort(floatToHalf(Color.green(px) / 255f))
//                buffer.putShort(floatToHalf(Color.blue(px) / 255f))
//            }
//        }
//        return buffer
//    }
//}
//

