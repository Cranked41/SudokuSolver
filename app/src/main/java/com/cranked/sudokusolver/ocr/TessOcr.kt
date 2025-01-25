package com.cranked.sudokusolver.ocr

import android.annotation.SuppressLint
import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cranked.sudokusolver.extensions.getAssetAsFile
import com.googlecode.tesseract.android.TessBaseAPI
import com.googlecode.tesseract.android.TessBaseAPI.PageSegMode
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class TessOcr(
    private val activity: Activity
) : TessBaseAPI.ProgressNotifier,
    OcrCamera.ResultListener {


    private var _errorLiveData = MutableLiveData<Boolean>()
    var errorLiveData: LiveData<Boolean> = _errorLiveData


    private var tessOcrFilePath: String? = null
    private var tessOcrFileDocPath: String? = null
    private var tessOcrFilePathName: String = "engbest"

    private fun initOcrPath() {
        tessOcrFileDocPath = activity.getExternalFilesDir("model")
            .toString()
        tessOcrFilePath = "$tessOcrFileDocPath/tessdata"
    }

    private var currentDirectory: File? = null
    private var mImageTextReader: ImageTextReader? = null
    private var mPageSegMode = 1
    lateinit var ocrCamera: OcrCamera



    fun initializeOcr() {
        initOcrPath()
        object : Thread() {
            override fun run() {
                try {
                    if (mImageTextReader != null) {
                        mImageTextReader!!.tearDownEverything()
                    }

                    mImageTextReader =
                        ImageTextReader.geInstance(
                            tessOcrFileDocPath,
                            tessOcrFilePathName,
                            mPageSegMode, this@TessOcr
                        )
                    if (!mImageTextReader!!.success) {
                        val destf = currentDirectory!!
                        destf.delete()
                        mImageTextReader = null
                    }

                    mImageTextReader?.let {
                        ocrCamera = OcrCamera(it, this@TessOcr)
                    } ?: run {

                        _errorLiveData.postValue(true)
                    }
                } catch (e: java.lang.Exception) {
                    mImageTextReader = null

                    _errorLiveData.postValue(true)
                }
            }
        }.start()
    }

    override fun onProgressValues(progressValues: TessBaseAPI.ProgressValues?) {
    }

    override fun ocrOnSuccess(text: String) {
        println("OcrSonucu:  $text")

    }


    override fun ocrOnError(exp: Exception?) {

        _errorLiveData.postValue(true)
    }
}