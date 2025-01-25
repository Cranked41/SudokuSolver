package com.cranked.sudokusolver

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivityViewModel : ViewModel() {
    private val coroutineException = CoroutineExceptionHandler { _, exception ->
        // -> CoroutineExceptionHandler
        println(exception.stackTraceToString())
    }
    private var _resultBitmap = MutableLiveData<Bitmap>()
    var resultBitmap: LiveData<Bitmap> = _resultBitmap

    fun sendResultBitmap(resultBitmap: Bitmap) {
        CoroutineScope(Dispatchers.IO + coroutineException).launch {
            _resultBitmap.postValue(resultBitmap)
        }
    }

}