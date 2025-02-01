package com.cranked.sudokusolver

import android.graphics.Bitmap
import android.graphics.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE
import org.opencv.imgproc.Imgproc.RETR_TREE
import org.opencv.imgproc.Imgproc.contourArea
import org.opencv.imgproc.Imgproc.findContours
import kotlin.coroutines.coroutineContext

class SudokuProcessor {

    suspend fun extractSudokuGrid(bitmap: Bitmap): Bitmap? =
        withContext(Dispatchers.IO + coroutineContext) {
        // OpenCV Mat'e dönüştür
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        // Gri tonlamaya dönüştür
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)

            // Gürültüyü azaltmak için Gaussian blur uygula
            val blurred = Mat()
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)

            // Kenarları belirginleştirmek için adaptif eşikleme uygula
        val thresh = Mat()
        Imgproc.adaptiveThreshold(
            blurred,
            thresh,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            11,
            2.0
        )

            // Kontur analizi ile Sudoku ızgarasını tespit et
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
            findContours(
            thresh,
            contours,
            hierarchy,
                Imgproc.RETR_EXTERNAL, CHAIN_APPROX_SIMPLE
        )

        var maxArea = 0.0
            var gridContour: MatOfPoint? = null

        for (contour in contours) {
            val area = contourArea(contour)
            if (area > maxArea && area > src.size()
                    .area() * 0.1
            ) { // Görüntünün %10'undan büyük konturları al
                val approx = MatOfPoint2f()
                val contour2f = MatOfPoint2f(*contour.toArray())
                val peri = Imgproc.arcLength(contour2f, true)
                Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true)

                if (approx.total().toInt() == 4) { // 4 köşeli ızgara konturu
                    maxArea = area
                    gridContour = MatOfPoint(*approx.toArray())
                }
            }
        }

            return@withContext (gridContour?.let {
                val srcPoints = sortCorners(it.toList())
                if (srcPoints.size == 4) {
                    // Perspektif düzeltme için hedef noktaları belirle
                    val rect = Imgproc.boundingRect(gridContour)
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

                    val warp = Mat()
                    Imgproc.warpPerspective(
                        gray,
                        warp,
                        transform,
                        Size(rect.width.toDouble(), rect.height.toDouble())
                    )

                    // Sonuç görüntüsünü Bitmap'e çevir ve döndür
                    val gridBitmap =
                        Bitmap.createBitmap(warp.cols(), warp.rows(), Bitmap.Config.ARGB_8888)
                    Utils.matToBitmap(warp, gridBitmap)
                    gridBitmap
                } else {
                    null
                }
            } ?: run { null })


        }

    // Köşe noktalarını saat yönünde sırala
    fun sortCorners(points: List<Point>): List<Point> {
        val center = Point(points.map { it.x }.average(), points.map { it.y }.average()
        )

        return points.sortedWith(compareBy({ Math.atan2(it.y - center.y, it.x - center.x) }))
    }

    fun flipBitmapHorizontally(bitmap: Bitmap): Bitmap {
        // Matrix nesnesi oluştur ve yatay eksende ölçekleme yap
        val matrix = Matrix().apply {
            preScale(-1f, 1f) // Yatayda ölçekleme (-1: ters çevirir)
        }

        // Bitmap'i yeni matrix ile yeniden oluştur
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    suspend fun extractSudokuCells(
        gridBitmap: Bitmap,
        paddingPercent: Float = 0.06f,
        offsetX: Int = 3,
        offsetY: Int = 3
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val src = Mat()
        Utils.bitmapToMat(gridBitmap, src)

        // Gri tonlamaya dönüştür
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)

        // Adaptif eşikleme uygula
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

        // Sudoku ızgarasını düzenli bir şekilde 9x9 karelere bölmek
        val cellWidth = src.width() / 9
        val cellHeight = src.height() / 9

        // Padding (kenar boşluğu) ekle
        val paddingX = (cellWidth * paddingPercent).toInt()
        val paddingY = (cellHeight * paddingPercent).toInt()

        val cells = mutableListOf<Bitmap>()

        for (row in 0 until 9) {
            for (col in 0 until 9) {
                // Hücrenin koordinatlarını hesapla (padding ve offsetY eklenmiş)
                val x = col * cellWidth + paddingX + offsetX
                val y = row * cellHeight + paddingY + offsetY // offsetY ile daha aşağıdan başla
                val width = cellWidth - 2 * paddingX
                val height = cellHeight - 2 * paddingY

                // Hücrenin sınırlarını kontrol et
                if (x >= 0 && y >= 0 && x + width <= src.width() && y + height <= src.height()) {
                    // Hücreyi kırp ve listeye ekle
                    val cell = Mat(src, Rect(x, y, width, height))
                    val cellBitmap =
                        Bitmap.createBitmap(cell.cols(), cell.rows(), Bitmap.Config.ARGB_8888)
                    Utils.matToBitmap(cell, cellBitmap)
                    cells.add(cellBitmap)
                } else {
                    // Hücre sınırların dışındaysa, boş bir bitmap ekle
                    cells.add(Bitmap.createBitmap(cellWidth, cellHeight, Bitmap.Config.ARGB_8888))
                }
            }
        }

        return@withContext cells
    }

    private fun findLargestContour(processingMat: Mat): MatOfPoint {
        val hierarchy = Mat()
        val contourList = mutableListOf<MatOfPoint>()

        findContours(
            processingMat, contourList, hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE
        )

        return contourList.maxBy { contourArea(it) }
    }


}
