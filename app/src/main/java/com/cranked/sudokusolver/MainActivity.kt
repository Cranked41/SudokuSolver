package com.cranked.sudokusolver

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cranked.sudokusolver.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis
import kotlin.time.measureTime

class MainActivity : AppCompatActivity() {
    private val sudoku = SudokuModel(
        arrayName = "sudoku", arrayOf(
            intArrayOf(3, 8, 0, 9, 0, 0, 2, 0, 5),
            intArrayOf(0, 0, 0, 0, 0, 8, 7, 3, 0),
            intArrayOf(0, 6, 0, 3, 0, 0, 9, 8, 0),
            intArrayOf(0, 0, 0, 0, 0, 3, 5, 0, 1),
            intArrayOf(9, 1, 0, 5, 0, 7, 0, 2, 3),
            intArrayOf(7, 0, 3, 1, 0, 0, 0, 0, 0),
            intArrayOf(0, 3, 5, 0, 0, 1, 0, 9, 0),
            intArrayOf(0, 7, 4, 6, 0, 0, 0, 0, 0),
            intArrayOf(8, 0, 1, 0, 0, 2, 0, 6, 7)
        )
    )
    val easySudoku = SudokuModel(
        arrayName = "easySudoku", arrayOf(
            intArrayOf(5, 3, 0, 0, 7, 0, 0, 0, 0),
            intArrayOf(6, 0, 0, 1, 9, 5, 0, 0, 0),
            intArrayOf(0, 9, 8, 0, 0, 0, 0, 6, 0),
            intArrayOf(8, 0, 0, 0, 6, 0, 0, 0, 3),
            intArrayOf(4, 0, 0, 8, 0, 3, 0, 0, 1),
            intArrayOf(7, 0, 0, 0, 2, 0, 0, 0, 6),
            intArrayOf(0, 6, 0, 0, 0, 0, 2, 8, 0),
            intArrayOf(0, 0, 0, 4, 1, 9, 0, 0, 5),
            intArrayOf(0, 0, 0, 0, 8, 0, 0, 7, 9)
        )
    )
    val mediumSudoku = SudokuModel(
        arrayName = "mediumSudoku", arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 2, 0, 0),
            intArrayOf(0, 0, 0, 6, 0, 0, 0, 0, 3),
            intArrayOf(0, 0, 0, 0, 7, 5, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 3, 0),
            intArrayOf(0, 5, 0, 1, 0, 2, 0, 6, 0),
            intArrayOf(0, 2, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 7, 3, 0, 0, 0, 0),
            intArrayOf(4, 0, 0, 0, 0, 1, 0, 0, 0),
            intArrayOf(0, 0, 9, 0, 0, 0, 0, 0, 0)
        )
    )
    val hardSudoku = SudokuModel(
        arrayName = "hardSudoku", arrayOf(
            intArrayOf(1, 0, 0, 0, 0, 7, 0, 9, 0),
            intArrayOf(0, 3, 0, 0, 2, 0, 0, 0, 8),
            intArrayOf(0, 0, 9, 6, 0, 0, 5, 0, 0),
            intArrayOf(0, 0, 5, 3, 0, 0, 9, 0, 0),
            intArrayOf(0, 1, 0, 0, 8, 0, 0, 0, 2),
            intArrayOf(6, 0, 0, 0, 0, 4, 0, 0, 0),
            intArrayOf(3, 0, 0, 0, 0, 0, 0, 1, 0),
            intArrayOf(0, 4, 0, 0, 0, 0, 0, 0, 7),
            intArrayOf(0, 0, 7, 0, 0, 0, 3, 0, 0)
        )
    )
    val hardSudoku1 = SudokuModel(
        arrayName = "hardSudoku1", arrayOf(
            intArrayOf(8, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 3, 6, 0, 0, 0, 0, 0),
            intArrayOf(0, 7, 0, 0, 9, 0, 2, 0, 0),
            intArrayOf(0, 5, 0, 0, 0, 7, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 4, 5, 7, 0, 0),
            intArrayOf(0, 0, 0, 1, 0, 0, 0, 3, 0),
            intArrayOf(0, 0, 1, 0, 0, 0, 0, 6, 8),
            intArrayOf(0, 0, 8, 5, 0, 0, 0, 1, 0),
            intArrayOf(0, 9, 0, 0, 0, 0, 4, 0, 0)
        )
    )
    val hardSudoku2 = SudokuModel(
        arrayName = "hardSudoku2", arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 1, 2),
            intArrayOf(0, 0, 0, 0, 0, 0, 7, 0, 0),
            intArrayOf(0, 0, 0, 4, 3, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 2, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 6, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 1, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 2, 5, 0, 0, 0),
            intArrayOf(0, 0, 6, 0, 0, 0, 0, 0, 0),
            intArrayOf(3, 7, 0, 0, 0, 0, 0, 0, 0)
        )
    )
    val hardSudoku3 = SudokuModel(
        arrayName = "hardSudoku3", arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 7, 0, 0, 0),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 3),
            intArrayOf(0, 0, 0, 0, 9, 0, 0, 4, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 2, 0, 0),
            intArrayOf(0, 7, 0, 0, 0, 0, 0, 3, 0),
            intArrayOf(0, 0, 8, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 3, 0, 0, 6, 0, 0, 0, 0),
            intArrayOf(4, 0, 0, 0, 0, 0, 0, 0, 6),
            intArrayOf(0, 0, 0, 8, 0, 0, 0, 0, 0)
        )
    )
    val hardSudoku4 = SudokuModel(
        arrayName = "hardSudoku4", arrayOf(
            intArrayOf(0, 0, 0, 7, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 5),
            intArrayOf(0, 0, 0, 0, 0, 6, 0, 0, 0),
            intArrayOf(0, 3, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(2, 0, 0, 8, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 4, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 1, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 3, 0),
            intArrayOf(0, 0, 0, 0, 5, 0, 0, 0, 0)
        )
    )
    val hardSudoku5 = SudokuModel(
        arrayName = "hardSudoku5", arrayOf(
            intArrayOf(0, 0, 5, 0, 0, 0, 0, 1, 0),
            intArrayOf(0, 0, 0, 0, 2, 0, 0, 0, 0),
            intArrayOf(0, 3, 0, 0, 0, 4, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 6, 0, 0),
            intArrayOf(0, 0, 0, 8, 0, 3, 0, 0, 0),
            intArrayOf(0, 0, 1, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 7, 0, 0, 0, 5, 0),
            intArrayOf(0, 0, 0, 0, 9, 0, 0, 0, 0),
            intArrayOf(0, 4, 0, 0, 0, 0, 2, 0, 0)
        )
    )
    val hardSudoku6 = SudokuModel(
        arrayName = "hardSudoku6", arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 8, 0),
            intArrayOf(0, 0, 0, 0, 0, 6, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 7, 0, 0, 0, 0),
            intArrayOf(0, 5, 0, 0, 0, 0, 4, 0, 0),
            intArrayOf(0, 0, 0, 1, 0, 9, 0, 0, 0),
            intArrayOf(0, 0, 3, 0, 0, 0, 0, 7, 0),
            intArrayOf(0, 0, 0, 0, 2, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 4, 0, 0, 0, 0, 0),
            intArrayOf(0, 1, 0, 0, 0, 0, 0, 0, 0)
        )
    )
    val hardSudoku7 = SudokuModel(
        arrayName = "hardSudoku7", arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 6, 0),
            intArrayOf(0, 0, 2, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 3, 0, 5, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 6, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 7, 0, 8, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 4, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 9, 0, 1, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 8, 0, 0),
            intArrayOf(0, 7, 0, 0, 0, 0, 0, 0, 0)
        )
    )
    val hardSudoku8 = SudokuModel(
        arrayName = "hardSudoku8", arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(0, 0, 0, 0, 0, 0, 6, 0, 0),
            intArrayOf(0, 0, 0, 4, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 8),
            intArrayOf(0, 0, 0, 0, 5, 0, 0, 0, 0),
            intArrayOf(0, 4, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 3, 0, 0, 0),
            intArrayOf(7, 0, 0, 0, 0, 0, 0, 0, 0)
        )
    )
    val hardSudoku9 = SudokuModel(
        arrayName = "hardSudoku9", arrayOf(
            intArrayOf(2, 4, 1, 6, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 8, 0, 0, 0, 0),
            intArrayOf(8, 0, 3, 0, 0, 0, 0, 1, 6),
            intArrayOf(9, 6, 0, 3, 0, 0, 8, 2, 0),
            intArrayOf(0, 7, 0, 9, 6, 8, 3, 0, 0),
            intArrayOf(0, 0, 0, 4, 0, 0, 6, 0, 0),
            intArrayOf(0, 1, 0, 8, 5, 2, 0, 6, 4),
            intArrayOf(7, 0, 6, 1, 0, 3, 9, 0, 0),
            intArrayOf(0, 0, 5, 0, 0, 0, 0, 0, 0)
        )
    )
    val hardSudoku10 = SudokuModel(
        arrayName = "hardSudoku10",arrayOf(
            intArrayOf(0, 7, 0, 0, 0, 0, 0, 4, 3),
            intArrayOf(0, 4, 0, 0, 0, 9, 6, 1, 0),
            intArrayOf(8, 0, 0, 6, 3, 4, 9, 0, 0),
            intArrayOf(0, 9, 4, 0, 5, 2, 0, 0, 0),
            intArrayOf(3, 5, 8, 4, 6, 0, 0, 2, 0),
            intArrayOf(0, 0, 0, 8, 0, 0, 5, 3, 0),
            intArrayOf(0, 8, 0, 0, 7, 0, 0, 9, 1),
            intArrayOf(9, 0, 2, 1, 0, 0, 0, 0, 5),
            intArrayOf(0, 0, 7, 0, 4, 0, 8, 0, 2)
        )
    )


    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initClickListener()
    }

    @SuppressLint("SetTextI18n")
    private fun initClickListener() {
        binding.solveButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val timeTaken = measureTimeMillis {
                    val selectedArray = arrayListOf(
                        sudoku,
                        easySudoku,
                        mediumSudoku,
                        hardSudoku,
                        hardSudoku1,
                        hardSudoku2,
                        hardSudoku3,
                        hardSudoku4,
                        hardSudoku5,
                        hardSudoku6,
                        hardSudoku7,
                        hardSudoku8,
                        hardSudoku9,
                        hardSudoku10,

                        ).random()
                    if (isValidSudoku(selectedArray.intArray)) {
                        if (solveSudokuAsync(selectedArray.intArray)) {
                            binding.sudokuOutput.text = selectedArray.arrayName
                            binding.sudokuResultImageView.setImageBitmap(
                                drawSudokuGrid(
                                    selectedArray.intArray
                                )
                            )
                        } else {
                            binding.sudokuOutput.text = "Sudoku çözülemedi."
                        }
                    } else {
                        binding.sudokuOutput.text =
                            "Sudoku ${selectedArray.arrayName} matrisi geçersiz!"
                    }
                }
                binding.timeTakenTextView.text = "Çözüm süresi ${timeTaken} ms"
            }
        }
    }

    // Asenkron Sudoku çözüm fonksiyonu
    private suspend fun solveSudokuAsync(board: Array<IntArray>): Boolean {
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


    private fun formatSudoku(board: Array<IntArray>): String {
        val builder = StringBuilder()
        for (row in board) {
            builder.append(row.joinToString(" ") { if (it == 0) "." else it.toString() })
            builder.append("\n")
        }
        return builder.toString()
    }
}