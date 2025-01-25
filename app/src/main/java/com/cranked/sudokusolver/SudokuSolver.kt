package com.cranked.sudokusolver

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SudokuSolver {
    suspend fun solveSudokuAsync(board: Array<IntArray>): Boolean {
        return withContext(Dispatchers.Default) {
            solveSudoku(board)
        }
    }


    fun isValidSudoku(grid: Array<IntArray>): Boolean {
        // Satır ve sütun kontrolü
        for (i in 0..8) {
            val rowSet = mutableSetOf<Int>()
            val colSet = mutableSetOf<Int>()
            for (j in 0..8) {
                // Satırda tekrar eden sayı kontrolü
                if (grid[i][j] != 0 && !rowSet.add(grid[i][j])) {
                    return false
                }
                // Sütunda tekrar eden sayı kontrolü
                if (grid[j][i] != 0 && !colSet.add(grid[j][i])) {
                    return false
                }
            }
        }

        // 3x3 blok kontrolü
        for (blockRow in 0..2) {
            for (blockCol in 0..2) {
                val blockSet = mutableSetOf<Int>()
                for (i in 0..2) {
                    for (j in 0..2) {
                        val value = grid[blockRow * 3 + i][blockCol * 3 + j]
                        if (value != 0 && !blockSet.add(value)) {
                            return false
                        }
                    }
                }
            }
        }
        return true
    }


    fun solveSudoku(board: Array<IntArray>): Boolean {
        for (row in 0..8) {
            for (col in 0..8) {
                if (board[row][col] == 0) {
                    for (num in 1..9) {
                        if (isSafe(board, row, col, num)) {
                            board[row][col] = num
                            if (solveSudoku(board)) {
                                return true
                            }
                            board[row][col] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    fun isSafe(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        // Satır kontrolü
        if (board[row].contains(num)) return false

        // Sütun kontrolü
        for (i in 0..8) {
            if (board[i][col] == num) return false
        }

        // 3x3 blok kontrolü
        val startRow = row - row % 3
        val startCol = col - col % 3
        for (i in 0..2) {
            for (j in 0..2) {
                if (board[startRow + i][startCol + j] == num) return false
            }
        }

        return true
    }

    fun drawSudokuGrid(sudoku: Array<IntArray>, cellSize: Int = 100): Bitmap {
        val gridSize = sudoku.size
        val bitmapSize = gridSize * cellSize

        // Create a blank Bitmap and Canvas
        val bitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE) // Set background color to white

        // Paint for grid lines
        val gridPaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 5f
            style = Paint.Style.STROKE
        }

        // Paint for numbers
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = cellSize / 2f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Draw grid lines
        for (i in 0..gridSize) {
            val position = i * cellSize
            canvas.drawLine(
                0f, position.toFloat(), bitmapSize.toFloat(), position.toFloat(), gridPaint
            ) // Horizontal lines
            canvas.drawLine(
                position.toFloat(), 0f, position.toFloat(), bitmapSize.toFloat(), gridPaint
            ) // Vertical lines
        }

        // Draw numbers in the grid
        val textOffset = (textPaint.descent() + textPaint.ascent()) / 2
        for (row in sudoku.indices) {
            for (col in sudoku[row].indices) {
                val value = sudoku[row][col]
                if (value != 0) {
                    val x = col * cellSize + cellSize / 2f
                    val y = row * cellSize + cellSize / 2f - textOffset
                    canvas.drawText(value.toString(), x, y, textPaint)
                }
            }
        }

        return bitmap
    }
}