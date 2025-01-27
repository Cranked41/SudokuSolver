package com.cranked.sudokusolver.tensorflow

import android.graphics.Bitmap

class SudokuGridProcessor {
    fun extractCellsFromBitmap(bitmap: Bitmap, coordinates: List<IntArray>): List<Bitmap> {
        val cells = mutableListOf<Bitmap>()
        for (coord in coordinates) {
            val (xMin, yMin, xMax, yMax) = coord
            val cropped = Bitmap.createBitmap(bitmap, xMin, yMin, xMax - xMin, yMax - yMin)
            cells.add(cropped)
        }
        return cells
    }
}