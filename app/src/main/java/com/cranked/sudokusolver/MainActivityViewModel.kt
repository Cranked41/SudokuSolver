package com.cranked.sudokusolver

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
    private var _resultBitmap = MutableLiveData<List<Bitmap>>()
    var resultBitmap: LiveData<List<Bitmap>> = _resultBitmap
    val solver = SudokuProcessor()

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
            val testRotateBitmap = drawableToBitmap(context.getDrawable(R.drawable.test15)!!)
            val imageSudokuGrid = solver.extractSudokuGrid(rotateBitmap)
            imageSudokuGrid?.let {
                val cells = solver.extractSudokuCells(it)
                if (cells.size == 81 && !sudokuLock) {
                    sudokuLock = true
                    _resultBitmap.postValue(
                        cells
                            .toList()
                    )

                }
            }
        }
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        // Eğer drawable zaten BitmapDrawable ise, direkt Bitmap döndür
        if (drawable is BitmapDrawable) {
            drawable.bitmap?.let { return it }
        }

        // Drawable'ın boyutlarını kontrol et
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1

        // Bitmap oluştur
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Drawable'ı Canvas kullanarak Bitmap'e çiz
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }
    fun convertHashMapToSudokuArray(sudokuResultHasMap: HashMap<Int, String>): Array<IntArray> {
        val sudokuArray = Array(9) { IntArray(9) } // 9x9 Sudoku dizisi oluştur

        for ((key, value) in sudokuResultHasMap) {
            val row = key / 9 // Satır numarası (0-8)
            val col = key % 9 // Sütun numarası (0-8)

            // String değeri Int'e çevir, çevrilemezse 0 ata
            sudokuArray[row][col] = value.toIntOrNull() ?: 0
        }

        return sudokuArray
    }
    fun initOcrVariable() {
        sudokuLock = false
    }


}