package com.cranked.sudokusolver.tensorflow

import android.annotation.SuppressLint
import android.content.Context
import android.util.Size
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
@SuppressLint("RestrictedApi")
object CameraSettings {
    fun processCameraProvider(context: Context): ProcessCameraProvider = ProcessCameraProvider.getInstance(context).get()

    fun cameraSelector() =
        CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

    fun preview(): Preview {
        return Preview.Builder().apply {
            setTargetResolution(Size(1920, 1080))
            setMaxResolution(Size(1920, 1080))
        }.build()
    }


    fun analysis(): ImageAnalysis {
        return ImageAnalysis.Builder().apply {
            setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            setTargetResolution(Size(1920, 1080))
            setMaxResolution(Size(1920, 1080))
        }.build()
    }
}