package com.cranked.sudokusolver.ocr

import android.annotation.SuppressLint
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * This class convert the image to text and return the text on image
 */
@SuppressLint("WrongConstant")
class ImageTextReader(
    val path: String?,
    val pathName: String?,
    val pageSegMode: Int = 1
) {

    private var api: TessBaseAPI? = null

    var success = false

    @Volatile
    var tessProcessing = false
    private val recycleLock = Any()

    init {
        api = TessBaseAPI()
        success = api!!.init(path, pathName)
        api!!.pageSegMode = pageSegMode
    }


    /**
     * get the text from bitmap
     *
     * @param request a DkOcrRequest
     * @return text on image
     */
    suspend fun getTextFromBitmap(bitmap: Bitmap): String? =
        withContext(Dispatchers.IO) {
            if (!success) return@withContext null
            if (tessProcessing) return@withContext null


            tessProcessing = true

            //Ocr Image
            val ocrbitmap = bitmap
            api?.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "123456789")
            api?.setImage(ocrbitmap)

            //OCR RESULT
            var textOnImage: String? = try {
                api?.utF8Text ?: ""
            } catch (e: Exception) {
                println("OcrHata$e")
                ""
            }
            api?.meanConfidence()?.let {
                if (it < 70) textOnImage = ""
            }
            //OCR CONFIDENCE RESULT


            api?.clear()
            synchronized(recycleLock) { tessProcessing = false }
            return@withContext textOnImage
        }

    /**
     * stop the image TEXT reader
     */
    fun stop() {
        api?.stop()
    }

    val accuracy: Int
        /**
         * find the confidence or
         *
         * @return confidence
         */
        get() = api?.meanConfidence() ?: 0

    /**
     * Closes down tesseract and free up all memory.
     */
    fun tearDownEverything() {
        api?.recycle()
    }

    /**
     * Frees up recognition results and any stored image data,
     */
    fun clearPreviousImage() {
        api?.clear()
    }
}