package com.cranked.sudokusolver.tensorflow

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class SudokuDetectionTF(context: Context, modelPath: String) {

    private val interpreter: Interpreter = Interpreter(File(modelPath))

    // Load TFLite model from assets
    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        FileInputStream(assetFileDescriptor.fileDescriptor).use { fis ->
            val fileChannel = fis.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }
    }


    // Görüntüyü hazırlama (28x28 boyutuna normalize etme)
    private fun preprocessImage(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 28, 28, true)
        val input = Array(1) { Array(28) { Array(28) { FloatArray(1) } } }

        for (y in 0 until 28) {
            for (x in 0 until 28) {
                val pixel = scaledBitmap.getPixel(x, y)
                val gray = (pixel and 0xFF).toFloat() / 255.0f // Gri tonlama
                input[0][y][x][0] = gray
            }
        }
        return input
    }

    // Modeli çalıştır ve koordinatları al
    fun detectSudokuCoordinates(bitmap: Bitmap): List<IntArray> {
        val output =
            Array(1) { FloatArray(324) }  // 81 kare * 4 koordinat (x_min, y_min, x_max, y_max)
        val input = preprocessImage(bitmap)
        interpreter.run(input, output)

        // Çıktıyı düzenle
        val coordinates = mutableListOf<IntArray>()
        for (i in 0 until 81) {
            val xMin = output[0][i * 4].toInt()
            val yMin = output[0][i * 4 + 1].toInt()
            val xMax = output[0][i * 4 + 2].toInt()
            val yMax = output[0][i * 4 + 3].toInt()
            if (xMin < xMax && yMin < yMax && xMin >= 0 && yMin >= 0) {
                coordinates.add(intArrayOf(xMin, yMin, xMax, yMax))
            }
        }
        return coordinates
    }


    // Crop a bitmap using coordinates
    fun cropBitmap(originalBitmap: Bitmap, coords: IntArray): Bitmap? {
        val xMin = coords[0].coerceAtLeast(0)
        val yMin = coords[1].coerceAtLeast(0)
        val xMax = coords[2].coerceAtMost(originalBitmap.width)
        val yMax = coords[3].coerceAtMost(originalBitmap.height)

        val width = xMax - xMin
        val height = yMax - yMin

        // Genişlik veya yükseklik 0 veya negatifse null döndür
        if (width <= 0 || height <= 0) {
            return null
        }

        return Bitmap.createBitmap(originalBitmap, xMin, yMin, width, height)
    }

    // Sudoku karelerinin Bitmap'lerini oluştur
    fun extractCellsFromBitmap(originalBitmap: Bitmap, coordinates: List<IntArray>): List<Bitmap> {
        val cells = mutableListOf<Bitmap>()
        for (coord in coordinates) {
            val (xMin, yMin, xMax, yMax) = coord
            val width = xMax - xMin
            val height = yMax - yMin

            // Genişlik veya yükseklik 0 veya negatifse null döndür
            if (width <= 0 || height <= 0) {
                continue
            }
            val croppedCell =
                Bitmap.createBitmap(originalBitmap, xMin, yMin, xMax - xMin, yMax - yMin)
            cells.add(croppedCell)
        }
        return cells
    }

    // Close interpreter when done
    fun close() {
        interpreter.close()
    }
}
