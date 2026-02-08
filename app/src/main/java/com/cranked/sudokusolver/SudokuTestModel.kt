package com.cranked.sudokusolver

class SudokuTestModel {

    private val sudoku = SudokuModel(
        arrayName = "sudoku", arrayOf(
            intArrayOf(0, 3, 0, 0, 5, 0, 4, 0, 7),
            intArrayOf(1, 0, 0, 0, 4, 0, 0, 0, 2),
            intArrayOf(9, 0, 0, 0, 7, 0, 0, 0, 0),
            intArrayOf(6, 0, 0, 0, 0, 8, 0, 0, 3),
            intArrayOf(0, 0, 0, 0, 0, 0, 9, 7, 0),
            intArrayOf(0, 9, 0, 0, 0, 2, 0, 6, 0),
            intArrayOf(0, 0, 5, 0, 2, 0, 0, 0, 6),
            intArrayOf(3, 0, 0, 0, 0, 1, 0, 0, 4),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0)
        )
    )
    private val sudokuFromGrid = SudokuModel(
        arrayName = "sudokuFromGrid", arrayOf(
            intArrayOf(0, 0, 0, 3, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 4, 0, 0, 0, 2, 8, 5),
            intArrayOf(0, 8, 1, 0, 0, 2, 0, 0, 0),
            intArrayOf(8, 0, 0, 0, 0, 0, 5, 9, 2),
            intArrayOf(0, 0, 0, 0, 0, 0, 7, 0, 0),
            intArrayOf(0, 0, 6, 0, 0, 9, 0, 0, 3),
            intArrayOf(0, 4, 0, 7, 3, 0, 0, 0, 9),
            intArrayOf(0, 9, 0, 8, 0, 0, 0, 0, 0),
            intArrayOf(0, 2, 0, 9, 0, 5, 1, 0, 7)
        )
    )
    private val easySudoku = SudokuModel(
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
    private val mediumSudoku = SudokuModel(
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
    private val hardSudoku = SudokuModel(
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
    private val hardSudoku1 = SudokuModel(
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
    private val hardSudoku2 = SudokuModel(
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
    private val hardSudoku3 = SudokuModel(
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
    private val hardSudoku4 = SudokuModel(
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
    private val hardSudoku5 = SudokuModel(
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
    private val hardSudoku6 = SudokuModel(
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
    private val hardSudoku7 = SudokuModel(
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
    private val hardSudoku8 = SudokuModel(
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
    private val hardSudoku9 = SudokuModel(
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
    private val hardSudoku10 = SudokuModel(
        arrayName = "hardSudoku10", arrayOf(
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
    val sudokuModels = arrayListOf(
        sudoku,
        sudokuFromGrid,
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
        hardSudoku10
    )
}