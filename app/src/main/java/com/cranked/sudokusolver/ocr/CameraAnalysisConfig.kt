package com.cranked.sudokusolver.ocr

import android.annotation.SuppressLint
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.impl.ImageOutputConfig.RotationValue

internal object CameraAnalysisConfig {

    @SuppressLint("UnsafeOptInUsageError", "RestrictedApi")
    fun imageBuilder(@RotationValue rotation: Int): ImageAnalysis {
        return ImageAnalysis.Builder()
            //.setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setMaxResolution(Size(1920, 1080))
            .setDefaultResolution(Size(1280,720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    fun previewBuilder(@RotationValue rotation: Int): Preview {
        return Preview.Builder()
            .setTargetRotation(rotation)
            .build()
    }

    /*
      Example imageAnalysis resolution
      //.setMaxResolution
      //.setDefaultResolution
      // Size(1920, 1080) ||| Size(1280,720) ||| Size(800, 600)
    */
}