package com.example.deteksiobatmu

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.deteksiobatmu.domain.DetectionResult
import com.example.deteksiobatmu.presentation.CameraPreview
import com.example.deteksiobatmu.presentation.MedicineImageAnalyzer
import com.example.deteksiobatmu.ui.theme.DeteksiObatMuTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var tts: TextToSpeech

    @SuppressLint("RememberReturnType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Minta izin kamera jika belum ada
        if (!hasCameraPermissions()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)
        }

        // Inisialisasi TTS
        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale("id", "ID")
            }
        }

        // Load model dan label
        val labels = assets.open("labels.txt").bufferedReader().readLines()
        val interpreter = Interpreter(loadModelFile(this, "modelYOLOv8n-5_float32.tflite"))

        setContent {
            DeteksiObatMuTheme {
                var detections by remember { mutableStateOf(emptyList<DetectionResult>()) }

                val lastSpeakTime = remember { mutableLongStateOf(0L) }
                val lastSpokenLabel = remember { mutableStateOf("") }
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                // ImageAnalyzer
                val analyzer = remember {
                    MedicineImageAnalyzer(
                        context = context,
                        interpreter = interpreter,
                        labels = labels,
                        onResults = { results ->
                            val now = System.currentTimeMillis()
                            detections = results
                            Log.d("MedicineAnalyzer", "Jumlah deteksi: ${results.size}")

                            scope.launch(Dispatchers.Default) {
                                if (results.isNotEmpty()) {
                                    val best = results.maxByOrNull { it.score }
                                    if (best != null &&
                                        (best.label != lastSpokenLabel.value || now - lastSpeakTime.longValue > 1000)
                                    ) {
                                        Log.d("MedicineAnalyzer", "Deteksi: ${best.label} (${best.score})")
                                        tts.speak(best.label, TextToSpeech.QUEUE_FLUSH, null, null)
                                        lastSpokenLabel.value = best.label
                                        lastSpeakTime.longValue = now
                                    }
                                } else if (now - lastSpeakTime.longValue > 1000) {
                                    Log.d("MedicineAnalyzer", "Tidak ada deteksi, ucapkan 'Siap'")
                                    withContext(Dispatchers.Main) {
                                        tts.speak("siap", TextToSpeech.QUEUE_FLUSH, null, null)
                                        lastSpokenLabel.value = ""
                                        lastSpeakTime.longValue = now
                                    }
                                }
                            }
                        }
                    )
                }

                // Controller kamera
                val controller = remember {
                    LifecycleCameraController(application).apply {
                        setEnabledUseCases(LifecycleCameraController.IMAGE_ANALYSIS)
                        setImageAnalysisAnalyzer(
                            ContextCompat.getMainExecutor(applicationContext),
                            analyzer
                        )
                    }
                }

                // =========================
                // UI
                // =========================
                Box(modifier = Modifier.fillMaxSize()) {
                    // Camera Preview
                    CameraPreview(
                        controller = controller,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Bounding box dan label
                    detections.forEach {
                        Box(
                            modifier = Modifier
                                .offset(x = it.box.left.dp, y = it.box.top.dp)
                                .size(width = it.box.width().dp, height = it.box.height().dp)
                                .border(2.dp, Color.Red)
                        ) {
                            androidx.compose.material3.Text(
                                text = "${it.label} ${(it.score * 100).toInt()}%",
                                fontSize = 20.sp,
                                color = Color.Red,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.5f))
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun hasCameraPermissions() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun loadModelFile(context: Context, fileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(fileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
    }
}
