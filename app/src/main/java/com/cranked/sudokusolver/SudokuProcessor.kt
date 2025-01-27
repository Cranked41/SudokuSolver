package com.cranked.sudokusolver



import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class SudokuProcessor {

    fun extractSudokuCells(bitmap: Bitmap): List<Bitmap> {
        // OpenCV Mat'e dönüştür
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        // Gri tonlamaya dönüştür
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)

        // Gaussian blur ile gürültüyü azalt
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

        // Kenar tespiti için adaptif eşikleme
        val thresh = Mat()
        Imgproc.adaptiveThreshold(
            gray,
            thresh,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            11,
            2.0
        )

        // En büyük konturu bulmak için kontur analizi
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            thresh,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        var maxArea = 0.0
        var sudokuContour: MatOfPoint? = null

        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > maxArea) {
                val approx = MatOfPoint2f()
                val contour2f = MatOfPoint2f(*contour.toArray())
                val peri = Imgproc.arcLength(contour2f, true)
                Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true)

                if (approx.total().toInt() == 4) {
                    maxArea = area
                    sudokuContour = MatOfPoint(*approx.toArray())
                }
            }
        }

        if (sudokuContour == null) {
            throw Exception("Sudoku gridi bulunamadı!")
        }

        // Sudoku gridini düzleştirme (perspektif düzeltme)
        val rect = Imgproc.boundingRect(sudokuContour)
        val warp = Mat()
        val srcPoints = sudokuContour.toList()
            .sortedBy { it.y }
            .let {
                listOf(
                    it[0],
                    it[1],
                    it[2],
                    it[3]
                )
            }
        val dstPoints = listOf(
            Point(0.0, 0.0),
            Point(rect.width.toDouble(), 0.0),
            Point(rect.width.toDouble(), rect.height.toDouble()),
            Point(0.0, rect.height.toDouble())
        )
        val transform = Imgproc.getPerspectiveTransform(
            MatOfPoint2f(*srcPoints.toTypedArray()),
            MatOfPoint2f(*dstPoints.toTypedArray())
        )
        Imgproc.warpPerspective(
            gray,
            warp,
            transform,
            Size(rect.width.toDouble(), rect.height.toDouble())
        )

        // Hücreleri ayır
        val cellWidth = warp.cols() / 9
        val cellHeight = warp.rows() / 9

        val cells = mutableListOf<Bitmap>()
        for (y in 0 until 9) {
            for (x in 0 until 9) {
                val cell = warp.submat(
                    y * cellHeight, (y + 1) * cellHeight,
                    x * cellWidth, (x + 1) * cellWidth
                )

                val cellBitmap =
                    Bitmap.createBitmap(cell.cols(), cell.rows(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(cell, cellBitmap)
                cells.add(cellBitmap)
            }
        }

        return cells
    }
}
