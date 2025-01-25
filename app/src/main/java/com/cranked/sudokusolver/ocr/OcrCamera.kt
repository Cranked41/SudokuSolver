package com.cranked.sudokusolver.ocr

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.text.Html
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis

class OcrCamera(
    private val mImageTextReader: ImageTextReader,
    private val resultListener: ResultListener
) {

    @SuppressLint("RestrictedApi")
    fun imageAnalysis(): ImageAnalysis {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            return ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setMaxResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
        }

        return ImageAnalysis.Builder()
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setMaxResolution(Size(1920, 1080))
            //.setMaxResolution(Size(1280, 720))
            .setTargetResolution(Size(1920, 1080))
            .setDefaultResolution(Size(1920, 1080))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    fun cameraSelector(): CameraSelector {
        return CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
    }

    private var releaseTextReader = true

    @SuppressLint("SetTextI18n")
    suspend fun convertImageToText(bitmap: Bitmap) {
        try {
            if (!releaseTextReader) return
            releaseTextReader = false

            val bitmapProcess = BitmapOcrUtils.preProcessBitmap(bitmap)
            val res = mImageTextReader.getTextFromBitmap(bitmapProcess)
            val cleanText = Html.fromHtml(res).toString().trim().replace(" ", "")
            resultListener.ocrOnSuccess(cleanText)
            releaseTextReader = true
        } catch (e: Exception) {
            resultListener.ocrOnError(e)
        }
    }

    interface ResultListener {
        fun ocrOnSuccess(text: String)
        fun ocrOnError(exp: Exception?)
    }
}