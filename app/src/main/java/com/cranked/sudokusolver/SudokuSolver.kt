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

    fun solveSudoku(board: Array<IntArray>): Boolean {
        if (hasThreeConsecutiveEmptyRowsOrColumns(board)) {
            return false
        }
        val fixedCells = getFixedCells(board) // Sabit hücreleri belirle

        for (row in 0..8) {
            for (col in 0..8) {
                // Sabit hücre ise atla, değiştirme!
                if (fixedCells.contains(Pair(row, col))) {
                    continue
                }

                if (board[row][col] == 0) { // Boş hücreyse sayı dene
                    for (num in 1..9) {
                        if (isSafe(board, row, col, num)) {
                            board[row][col] = num
                            if (solveSudoku(board) && isValidSudoku(board)) { // Sudoku geçerli mi kontrol et
                                return true
                            }
                            board[row][col] = 0 // Geri alma (backtracking)
                        }
                    }
                    return false // Çözüm bulunamazsa geri adım at
                }
            }
        }

        // Çözüm tamamlandıktan sonra Sudoku kurallarına uygun mu kontrol et
        return isValidSudoku(board)
    }

    // **Sabit hücreleri belirleyen fonksiyon**
    fun getFixedCells(board: Array<IntArray>): Set<Pair<Int, Int>> {
        val fixedCells = mutableSetOf<Pair<Int, Int>>()
        for (row in 0..8) {
            for (col in 0..8) {
                if (board[row][col] != 0) {
                    fixedCells.add(Pair(row, col))
                }
            }
        }
        return fixedCells
    }

    // **Sudoku kurallarına uygunluğu kontrol eden fonksiyon**
    fun isSafe(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        // Satır kontrolü
        if (board[row].contains(num)) return false

        // Sütun kontrolü
        for (i in 0..8) {
            if (board[i][col] == num) return false
        }

        // 3x3 kutu kontrolü
        val boxRowStart = (row / 3) * 3
        val boxColStart = (col / 3) * 3
        for (i in 0..2) {
            for (j in 0..2) {
                if (board[boxRowStart + i][boxColStart + j] == num) return false
            }
        }

        return true
    }

    // Eğer bütün satırlar VE bütün sütunlar tamamen 0 ise true döner
    // Eğer 3 satır VEYA 3 sütun art arda tamamen 0 ise true döner
    fun hasThreeConsecutiveEmptyRowsOrColumns(board: Array<IntArray>): Boolean {

        // --- SATIR KONTROLÜ ---
        var emptyRowCount = 0
        for (row in 0..8) {
            val isRowEmpty = board[row].all { it == 0 }

            if (isRowEmpty) {
                emptyRowCount++
                if (emptyRowCount == 3) return true
            } else {
                emptyRowCount = 0
            }
        }

        // --- SÜTUN KONTROLÜ ---
        var emptyColCount = 0
        for (col in 0..8) {
            var isColEmpty = true
            for (row in 0..8) {
                if (board[row][col] != 0) {
                    isColEmpty = false
                    break
                }
            }

            if (isColEmpty) {
                emptyColCount++
                if (emptyColCount == 3) return true
            } else {
                emptyColCount = 0
            }
        }

        return false
    }

    // **Sudoku'nun geçerli olup olmadığını kontrol eden fonksiyon**
    fun isValidSudoku(grid: Array<IntArray>): Boolean {
        // Satır ve sütun kontrolü
        for (i in 0..8) {
            val rowSet = mutableSetOf<Int>()
            val colSet = mutableSetOf<Int>()
            for (j in 0..8) {
                // Satırda tekrar eden sayı kontrolü
                if (grid[i][j] != 0) {
                    if (grid[i][j] !in 1..9 || !rowSet.add(grid[i][j])) {
                        return false
                    }
                }
                // Sütunda tekrar eden sayı kontrolü
                if (grid[j][i] != 0) {
                    if (grid[j][i] !in 1..9 || !colSet.add(grid[j][i])) {
                        return false
                    }
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
                        if (value != 0) {
                            if (!blockSet.add(value)) {
                                return false
                            }
                        }
                    }
                }
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