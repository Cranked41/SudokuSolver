package com.cranked.sudokusolver

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cranked.sudokusolver.extensions.getAssetAsFile
import com.cranked.sudokusolver.tensorflow.SudokuDetectionTF
import com.cranked.sudokusolver.utils.maze.ImageUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivityViewModel : ViewModel() {
    private var sudokuLock = false
    private val coroutineException = CoroutineExceptionHandler { _, exception ->
        // -> CoroutineExceptionHandler
        println(exception.stackTraceToString())
        sudokuLock = false
    }
    private var sudokuDetectionTF: SudokuDetectionTF? = null
    private var _resultBitmap = MutableLiveData<Bitmap>()
    var resultBitmap: LiveData<Bitmap> = _resultBitmap

    fun sendResultBitmap(resultBitmap: Bitmap) {
        CoroutineScope(Dispatchers.IO + coroutineException).launch {}
    }

    fun checkSudoku(
        context: Context, bitmap: Bitmap, rotationDegrees: Float = 90f
    ) {
        CoroutineScope(Dispatchers.IO + coroutineException).launch {
            if (sudokuLock) {
                return@launch
            }
            val rotateBitmap =
                ImageUtil.rotateImage(bitmap = bitmap, rotationDegrees = rotationDegrees)

            val modelPath = context.applicationContext.getExternalFilesDir("model")
                .toString() + File.separator + "sudoku_model.tflite"
            sudokuLock = true
            if (sudokuDetectionTF == null) {
                sudokuDetectionTF = SudokuDetectionTF(context, modelPath)
            }
            // Koordinatları algılayın
            //  val coordinates = sudokuDetectionTF!!.detectSudokuCoordinates(rotateBitmap)

            // Kareleri kesmek için koordinatları kullanın
            /* val croppedCell = sudokuDetectionTF!!.extractCellsFromBitmap(rotateBitmap, coordinates)
             croppedCell.forEach {
                 _resultBitmap.postValue(it)
             }

             */
            // Kesilen karelerle işleme devam edin

            sudokuLock = false
        }
    }


}