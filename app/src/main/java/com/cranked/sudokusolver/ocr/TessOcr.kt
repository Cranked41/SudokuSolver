package com.cranked.sudokusolver.ocr

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.googlecode.tesseract.android.TessBaseAPI
import com.googlecode.tesseract.android.TessBaseAPI.PageSegMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class TessOcr(
    private val activity: Activity
) : TessBaseAPI.ProgressNotifier,
    OcrCamera.ResultListener {


    private var _errorLiveData = MutableLiveData<Boolean>()
    var errorLiveData: LiveData<Boolean> = _errorLiveData


    private var tessOcrFilePath: String? = null
    private var tessOcrFileDocPath: String? = null
    private var tessOcrFilePathName: String = "eng"

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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (mImageTextReader != null) {
                    mImageTextReader!!.tearDownEverything()
                }

                mImageTextReader =
                    ImageTextReader(
                        tessOcrFileDocPath,
                        tessOcrFilePathName,
                        PageSegMode.PSM_SINGLE_WORD
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
    }

    override fun onProgressValues(progressValues: TessBaseAPI.ProgressValues?) {
    }

    override fun ocrOnSuccess(text: String) {

    }


    override fun ocrOnError(exp: Exception?) {

        _errorLiveData.postValue(true)
    }
}