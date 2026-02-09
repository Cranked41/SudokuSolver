package com.cranked.sudokusolver.ocr

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import com.cranked.sudokusolver.model.OcrResultModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OcrCamera(
    val mImageTextReader: ImageTextReader,
) {


    @SuppressLint("SetTextI18n")
    suspend fun convertImageToText(bitmap: Bitmap): OcrResultModel? = withContext(Dispatchers.IO) {

        return@withContext bitmap.let {
            mImageTextReader.getTextFromBitmap(it) ?: run {
                OcrResultModel("", 0)
            }
        }


    }
}