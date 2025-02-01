package com.cranked.sudokusolver.ocr

import android.graphics.Bitmap
import com.cranked.sudokusolver.extensions.resizeBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max

class MlKitOcrHelper {
    val textRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.Builder().build()) // Varsayılan ayarlarla

    suspend fun recognizeTextFromBitmap(bitmap: Bitmap): String? =
        suspendCoroutine { continuation ->
            // Bitmap'i InputImage'e dönüştür
            val image = InputImage.fromBitmap(
                bitmap.resizeBitmap(
                    max(32, bitmap.width),
                    max(32, bitmap.height)
                ), 0
            )

            // Metin tanıma işlemini başlat
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // Tanınan metni döndür
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    // Hata durumunda
                    continuation.resumeWithException(e)
                }
        }

    fun close() {
        textRecognizer.close()
    }
}