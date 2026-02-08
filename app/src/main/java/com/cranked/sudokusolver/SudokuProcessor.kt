package com.cranked.sudokusolver

import android.graphics.Bitmap
import android.graphics.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE
import org.opencv.imgproc.Imgproc.RETR_TREE
import org.opencv.imgproc.Imgproc.contourArea
import org.opencv.imgproc.Imgproc.findContours
import kotlin.math.roundToInt
import androidx.core.graphics.createBitmap
import com.cranked.sudokusolver.model.SudokuOcrModel

class SudokuProcessor {

    suspend fun extractSudokuGrid(bitmap: Bitmap): Bitmap? =
        withContext(Dispatchers.IO) {
            // OpenCV Mat'e dönüştür
            val src = Mat()
            Utils.bitmapToMat(bitmap, src)

            // 📌 Gri tonlamaya çevir
            val gray = Mat()
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)

            // 📌 Gürültüyü azalt (Daha iyi tespit için)
            Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

            // 📌 Adaptif eşikleme (Python'daki gibi)
            val thresh = Mat()
            Imgproc.adaptiveThreshold(
                gray, thresh, 255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV, 11, 2.0
            )

            // 📌 Kontur analizi ile Sudoku ızgarasını tespit et
            val contours = mutableListOf<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

            var maxArea = 0.0
            var gridContour: MatOfPoint? = null

            for (contour in contours) {
                val area = Imgproc.contourArea(contour)
                if (area > maxArea && area > src.size().area() * 0.1) { // Görüntünün %10'undan büyük konturları al
                    val approx = MatOfPoint2f()
                    val contour2f = MatOfPoint2f(*contour.toArray())
                    val peri = Imgproc.arcLength(contour2f, true)
                    Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true)

                    if (approx.total().toInt() == 4) { // 📌 4 KÖŞELİ DIKDÖRTGEN BULURSA KABUL ET
                        maxArea = area
                        gridContour = MatOfPoint(*approx.toArray())
                    }
                }
            }

            return@withContext (gridContour?.let {
                val srcPoints = sortCorners(it.toList())
                if (srcPoints.size == 4) {
                    // 📌 Perspektif düzeltme için hedef noktaları belirle
                    val dstPoints = listOf(
                        Point(0.0, 0.0),
                        Point(900.0, 0.0),
                        Point(900.0, 900.0),
                        Point(0.0, 900.0)
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
                        Size(900.0, 900.0)
                    )

                    // 📌 Sonuç görüntüsünü Bitmap'e çevir ve döndür
                    val gridBitmap = Bitmap.createBitmap(900, 900, Bitmap.Config.ARGB_8888)
                    Utils.matToBitmap(warp, gridBitmap)

                    // 📌 Sudoku'nun yönünü belirleyip düzelt
                    return@let fixSudokuOrientation(gridBitmap, srcPoints)
                } else {
                    null
                }
            } ?: run { null })
        }

    // 📌 Köşe noktalarını [sol üst, sağ üst, sağ alt, sol alt] (TL, TR, BR, BL) sırasına göre düzenler
    fun sortCorners(points: List<Point>): List<Point> {
        if (points.size != 4) return points

        // TL: (x + y) en küçük, BR: (x + y) en büyük
        val tl = points.minByOrNull { it.x + it.y } ?: return points
        val br = points.maxByOrNull { it.x + it.y } ?: return points

        // Kalan iki nokta: TR ve BL
        val remaining = points.filter { it != tl && it != br }
        if (remaining.size != 2) return points

        // TR: (y - x) en küçük, BL: (y - x) en büyük
        val tr = remaining.minByOrNull { it.y - it.x } ?: return points
        val bl = remaining.maxByOrNull { it.y - it.x } ?: return points

        return listOf(tl, tr, br, bl)
    }
    // 📌 Bitmap'i belirli bir açıyla döndürme fonksiyonu
    fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
    // 📌 Sudoku'nun yönünü algılayıp otomatik düzelten fonksiyon
    fun fixSudokuOrientation(bitmap: Bitmap, corners: List<Point>): Bitmap {
        // NOT: Bu fonksiyon daha önce orijinal görüntünün en/boy oranına göre 90° döndürme yapıyordu.
        // Sudoku ızgarasını zaten 900x900'e warpPerspective ile düzleştiriyoruz.
        // Bu yüzden burada 90° döndürme yapmak çoğu durumda gereksiz ve "sağa yatık"/yan çıkma problemine sebep olabiliyor.

        // Eğer gerçekten ters (180°) duruyorsa, bunu köşe sırasından anlamaya çalışıp sadece 180° düzelt.
        // Aksi halde bitmap'i olduğu gibi döndür.
        val tl = corners.getOrNull(0) ?: return bitmap
        val tr = corners.getOrNull(1) ?: return bitmap

        // Top edge sağa doğru gidiyorsa normal (tr.x > tl.x). Değilse 180° çevir.
        return if (tr.x < tl.x) rotateBitmap(bitmap, 180f) else bitmap
    }
    // 📌 3x3 büyük kareler için veri modeli
    data class SudokuBlockModel(
        val bitmap: Bitmap, // 3x3 büyük kare görüntüsü
        val rect: Rect      // Koordinatları
    )

    suspend fun extract3x3Blocks(bitmap: Bitmap): List<SudokuBlockModel>? =
        withContext(Dispatchers.IO) {
            val gridBitmap = extractSudokuGrid(bitmap) ?: return@withContext null // Önce Sudoku Gridini Al

            val sudokuMat = Mat()
            Utils.bitmapToMat(gridBitmap, sudokuMat)

            val blockSize = sudokuMat.rows() / 3 // Her büyük kare için boyut hesapla
            val padding = 10                     // Kenarlardan içeriye doğru küçültme

            val blocks = mutableListOf<SudokuBlockModel>()

            for (row in 0 until 3) {
                for (col in 0 until 3) {
                    val x1 = col * blockSize + padding
                    val y1 = row * blockSize + padding
                    val x2 = (col + 1) * blockSize - padding
                    val y2 = (row + 1) * blockSize - padding

                    if (x1 < 0 || y1 < 0 || x2 > sudokuMat.cols() || y2 > sudokuMat.rows()) {
                        continue // Geçersizse atla
                    }

                    // 📌 Büyük kareyi kes
                    val croppedBlock = Mat(sudokuMat, Rect(x1, y1, x2 - x1, y2 - y1))

                    // 📌 Bitmap'e çevir
                    val croppedBitmap = Bitmap.createBitmap(croppedBlock.cols(), croppedBlock.rows(), Bitmap.Config.ARGB_8888)
                    Utils.matToBitmap(croppedBlock, croppedBitmap)

                    blocks.add(SudokuBlockModel(croppedBitmap, Rect(x1, y1, x2 - x1, y2 - y1)))
                }
            }

            return@withContext blocks
        }
    suspend fun detect3x3Blocks(bitmap: Bitmap): List<SudokuBlockModel> =
        withContext(Dispatchers.IO) {
            val srcMat = Mat()
            Utils.bitmapToMat(bitmap, srcMat)

            // 📌 Gri tonlamaya çevir
            val gray = Mat()
            Imgproc.cvtColor(srcMat, gray, Imgproc.COLOR_BGR2GRAY)

            // 📌 Gürültüyü azalt
            Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

            // 📌 Eşikleme (Adaptif Threshold)
            val binary = Mat()
            Imgproc.adaptiveThreshold(
                gray, binary, 255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV, 11, 2.0
            )

            // 📌 Kenarları netleştirme
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
            Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_CLOSE, kernel)

            // 📌 Kontur analizi ile 3x3 büyük kareleri tespit et
            val contours = mutableListOf<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

            val blocks = mutableListOf<SudokuBlockModel>()

            for (contour in contours) {
                val rect = Imgproc.boundingRect(contour)

                // 📌 Karelerin yaklaşık boyutunu belirle (3x3 büyük kare olmalı)
                val aspectRatio = rect.width.toDouble() / rect.height.toDouble()
                if (aspectRatio > 0.8 && aspectRatio < 1.2 && rect.width > 50 && rect.width < 300) {
                    val croppedBlock = srcMat.submat(rect)
                    val croppedBitmap = Bitmap.createBitmap(croppedBlock.cols(), croppedBlock.rows(), Bitmap.Config.ARGB_8888)
                    Utils.matToBitmap(croppedBlock, croppedBitmap)

                    blocks.add(SudokuBlockModel(croppedBitmap, rect))
                }
            }

            // 📌 3x3 büyük kareleri sırayla döndür
            return@withContext blocks.sortedBy { it.rect.y * 1000 + it.rect.x }
        }
    fun flipBitmapHorizontally(bitmap: Bitmap): Bitmap {
        // Matrix nesnesi oluştur ve yatay eksende ölçekleme yap
        val matrix = Matrix().apply {
            preScale(-1f, 1f) // Yatayda ölçekleme (-1: ters çevirir)
        }

        // Bitmap'i yeni matrix ile yeniden oluştur
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    fun clamp(v: Int, min: Int, max: Int) = v.coerceIn(min, max)

    // Hücre boş mu? (rakam yok) — OCR'ı atlamak için hızlı kontrol
    fun isCellEmpty(cellGray: Mat): Boolean {
        // 1) Hafif blur (gürültüyü azalt)
        val blur = Mat()
        Imgproc.GaussianBlur(cellGray, blur, Size(3.0, 3.0), 0.0)

        // 2) Binary (digit = white)
        val bin = Mat()
        Imgproc.adaptiveThreshold(
            blur, bin, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV, 11, 2.0
        )

        // 3) Kenar (grid çizgisi) etkisini azalt: border'ı sıfırla
        // (ince '7' gibi rakamları öldürmeden çizgi gürültüsünü azaltır)
        val pad = (minOf(bin.rows(), bin.cols()) * 0.10).toInt().coerceAtLeast(2)
        // Üst / alt
        bin.rowRange(0, pad).setTo(Scalar(0.0))
        bin.rowRange(bin.rows() - pad, bin.rows()).setTo(Scalar(0.0))
        // Sol / sağ
        bin.colRange(0, pad).setTo(Scalar(0.0))
        bin.colRange(bin.cols() - pad, bin.cols()).setTo(Scalar(0.0))

        // 4) Çok hafif close (stroke'ları birleştirir, 7 gibi ince rakamlarda yardımcı)
        val k = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(2.0, 2.0))
        Imgproc.morphologyEx(bin, bin, Imgproc.MORPH_CLOSE, k)

        // 5) Hızlı yoğunluk ölçümü
        val nonZero = Core.countNonZero(bin)
        val total = bin.rows() * bin.cols()
        val density = nonZero.toDouble() / total.toDouble()

        // 6) Bağlı bileşen analizi: ince rakamları (örn. 7) yakalamak için geometri kontrolü
        val labels = Mat()
        val stats = Mat()
        val centroids = Mat()
        val n = Imgproc.connectedComponentsWithStats(bin, labels, stats, centroids)

        val hMin = (bin.rows() * 0.25).toInt().coerceAtLeast(6)   // rakam boyu en az hücrenin %25'i
        val wMin = (bin.cols() * 0.08).toInt().coerceAtLeast(3)   // rakam eni en az hücrenin %8'i
        val minArea = (total * 0.002).toInt().coerceAtLeast(12)   // çok küçük noktaları ele

        var hasValidDigit = false
        var maxArea = 0

        for (i in 1 until n) { // 0 = background
            val area = stats.get(i, Imgproc.CC_STAT_AREA)?.get(0)?.toInt() ?: 0
            if (area > maxArea) maxArea = area
            if (area < minArea) continue

            val w = stats.get(i, Imgproc.CC_STAT_WIDTH)?.get(0)?.toInt() ?: 0
            val h = stats.get(i, Imgproc.CC_STAT_HEIGHT)?.get(0)?.toInt() ?: 0

            // BBox çok küçükse gürültü
            if (w < wMin || h < hMin) continue

            // BBox içi doluluk oranı: aşırı düşükse (tek çizgi gürültüsü) ele
            val fill = area.toDouble() / (w.toDouble() * h.toDouble()).coerceAtLeast(1.0)
            if (fill < 0.03) continue

            hasValidDigit = true
            break
        }

        // cleanup
        blur.release()
        bin.release()
        k.release()
        labels.release()
        stats.release()
        centroids.release()

        // Karar:
        // - Hiç geçerli bileşen yoksa boş
        // - Yoğunluk çok düşük olsa bile (7 gibi) geçerli bileşen varsa DOLU
        // - Yoğunluk aşırı yüksekse zaten doludur (kalın yazı / gölge)
        if (hasValidDigit) return false
        if (density > 0.10) return false
        return density < 0.02
    }

    suspend fun extractSudokuCells(
        gridBitmap: Bitmap,
        paddingPercent: Float = 0.08f
    ): List<SudokuOcrModel> = withContext(Dispatchers.IO) {
        val src = Mat()
        Utils.bitmapToMat(gridBitmap, src)

        // Gri tonlamaya dönüştür (hücre ROI'leri daha stabil olur)
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)

        val w = gray.cols().toDouble()
        val h = gray.rows().toDouble()

        // Hücre boyutlarını double olarak hesapla (integer truncation kayması olmasın)
        val cellW = w / 9.0
        val cellH = h / 9.0

        // Hücre içi marj (kenar çizgilerini ve sınır kaymalarını azaltır)
        val marginX = (cellW * paddingPercent).toInt().coerceAtLeast(1)
        val marginY = (cellH * paddingPercent).toInt().coerceAtLeast(1)


        val cells = ArrayList<SudokuOcrModel>(81)

        for (row in 0 until 9) {
            for (col in 0 until 9) {
                // Sınırları rounding ile bul: (col*cellW) -> (col+1)*cellW
                val x0 = (col * cellW).roundToInt().toInt()
                val x1 = ((col + 1) * cellW).roundToInt().toInt()
                val y0 = (row * cellH).roundToInt().toInt()
                val y1 = ((row + 1) * cellH).roundToInt().toInt()

                // Marj uygula
                var x = x0 + marginX
                var y = y0 + marginY
                var ww = (x1 - x0) - 2 * marginX
                var hh = (y1 - y0) - 2 * marginY

                // Güvenli clamp
                x = clamp(x, 0, gray.cols() - 1)
                y = clamp(y, 0, gray.rows() - 1)
                ww = clamp(ww, 1, gray.cols() - x)
                hh = clamp(hh, 1, gray.rows() - y)

                val cellMat = Mat(gray, Rect(x, y, ww, hh))

                // Hücre boş mu kontrol et (OCR'ı dışarıda atlayacağız)
                val empty = isCellEmpty(cellMat)

                // Bitmap'e çevir
                val cellBitmap = createBitmap(cellMat.cols(), cellMat.rows())
                Utils.matToBitmap(cellMat, cellBitmap)

                // Eğer boşsa notOcr=true, doluysa notOcr=false
                cells.add(SudokuOcrModel(notOcr = empty, cellBitmap = cellBitmap))

                cellMat.release()
            }
        }

        return@withContext cells
    }
    fun detectSudokuCorners(gridBitmap: Bitmap): List<Point> {
        val src = Mat()
        Utils.bitmapToMat(gridBitmap, src)

        // Gri tonlamaya çevir ve bulanıklaştır
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

        // Adaptif eşikleme uygula (gerekiyorsa bitwise_not ile ters çevirme)
        Imgproc.adaptiveThreshold(
            gray, gray, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY, 11, 2.0
        )
        Core.bitwise_not(gray, gray)

        // Konturları bul
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(gray, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        // En büyük alanlı konturu sudoku ızgarası olarak seç
        var maxArea = 0.0
        var sudokuContour: MatOfPoint? = null
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > maxArea) {
                maxArea = area
                sudokuContour = contour
            }
        }

        // Eğer hiç kontur bulunamadıysa, fallback olarak tüm görüntünün köşelerini kullan
        if (sudokuContour == null) {
            val tl = Point(0.0, 0.0)
            val tr = Point(gridBitmap.width.toDouble(), 0.0)
            val br = Point(gridBitmap.width.toDouble(), gridBitmap.height.toDouble())
            val bl = Point(0.0, gridBitmap.height.toDouble())
            return listOf(tl, tr, br, bl)
        }

        // Konturu çokgenle yaklaşıklaştır (4 köşe elde etmeyi bekliyoruz)
        val peri = Imgproc.arcLength(MatOfPoint2f(*sudokuContour.toArray()), true)
        val approx = MatOfPoint2f()
        Imgproc.approxPolyDP(MatOfPoint2f(*sudokuContour.toArray()), approx, 0.02 * peri, true)

        // Eğer dört nokta elde edilemediyse, fallback olarak konturun boundingRect'ini kullan
        if (approx.total().toInt() != 4) {
            val boundingRect = Imgproc.boundingRect(sudokuContour)
            val tl = Point(boundingRect.x.toDouble(), boundingRect.y.toDouble())
            val tr = Point((boundingRect.x + boundingRect.width).toDouble(), boundingRect.y.toDouble())
            val br = Point((boundingRect.x + boundingRect.width).toDouble(), (boundingRect.y + boundingRect.height).toDouble())
            val bl = Point(boundingRect.x.toDouble(), (boundingRect.y + boundingRect.height).toDouble())
            return listOf(tl, tr, br, bl)
        }

        val points = approx.toArray().toList()
        return orderPoints(points)
    }

    /**
     * 4 adet nokta içeren listeyi [sol üst, sağ üst, sağ alt, sol alt] sırasına göre sıralar.
     */
    fun orderPoints(points: List<Point>): List<Point> {
        // Sol üst nokta: x+y toplamı en düşük, sağ alt: x+y toplamı en yüksek
        val tl = points.minByOrNull { it.x + it.y }!!
        val br = points.maxByOrNull { it.x + it.y }!!

        // Kalan 2 nokta; x koordinatına göre sıralayıp sağ üst ve sol alt belirlenir.
        val remaining = points.filter { it != tl && it != br }
        val (tr, bl) = if (remaining[0].x > remaining[1].x)
            Pair(remaining[0], remaining[1])
        else
            Pair(remaining[1], remaining[0])

        return listOf(tl, tr, br, bl)
    }

    private fun findLargestContour(processingMat: Mat): MatOfPoint {
        val hierarchy = Mat()
        val contourList = mutableListOf<MatOfPoint>()

        findContours(
            processingMat, contourList, hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE
        )

        return contourList.maxBy { contourArea(it) }
    }
    /**
     * Sudoku ızgarasından, gerçek köşe koordinatlarına dayalı olarak her hücreyi (9x9 = 81 adet)
     * ayrı Bitmap olarak çıkarır.
     */
    // Yardımcı: İki nokta arasındaki Öklid mesafesini hesaplar.
    private fun distance(p1: Point, p2: Point): Double {
        return Math.hypot(p1.x - p2.x, p1.y - p2.y)
    }

    /**
     * Sudoku ızgarasını düzleştirip (perspektif düzeltme) yüksek çözünürlüklü hale getirir,
     * sonrasında 9x9 hücrelere böler.
     */
    suspend fun extractSudokuCellsFromWarpedGrid(
        gridBitmap: Bitmap,
        cellSize: Int = 28,            // Son hedef hücre boyutu (örneğin 28x28 piksel)
        marginRatio: Double = 0.05     // Hücre içi marj oranı (örn. %5)
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        // Sudoku ızgarasının köşe noktalarını tespit edin (sıralama: [top-left, top-right, bottom-right, bottom-left])
        val gridCorners = detectSudokuCorners(gridBitmap)
        // gridCorners dizisinde her nokta tipik olarak Point tipindedir.

        // Girdi görüntüsünü Mat formatına dönüştürün
        val gridMat = Mat()
        Utils.bitmapToMat(gridBitmap, gridMat)

        // Orijinal köşe noktalarından genişlik ve yükseklik hesaplayın
        val widthA = distance(gridCorners[0], gridCorners[1])
        val widthB = distance(gridCorners[3], gridCorners[2])
        val maxWidth = Math.max(widthA, widthB)

        val heightA = distance(gridCorners[0], gridCorners[3])
        val heightB = distance(gridCorners[1], gridCorners[2])
        val maxHeight = Math.max(heightA, heightB)

        // Gerekirse, daha yüksek çözünürlükte çalışmak için ölçek faktörü uygulayın
        val upscaleFactor = 4  // Bu değeri ihtiyaca göre ayarlayın
        val warpedWidth = (maxWidth * upscaleFactor).toInt()
        val warpedHeight = (maxHeight * upscaleFactor).toInt()

        // Hedef (düz) görüntü için köşe noktalarını belirleyin
        val dstCorners = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(warpedWidth.toDouble(), 0.0),
            Point(warpedWidth.toDouble(), warpedHeight.toDouble()),
            Point(0.0, warpedHeight.toDouble())
        )

        // Kaynak köşe noktalarını oluşturun (tespit ettiğimiz gridCorners)
        val srcCorners = MatOfPoint2f(
            gridCorners[0],
            gridCorners[1],
            gridCorners[2],
            gridCorners[3]
        )

        // Perspektif dönüşümünü hesaplayın ve tüm ızgarayı düzleştirin
        val transform = Imgproc.getPerspectiveTransform(srcCorners, dstCorners)
        val warped = Mat()
        Imgproc.warpPerspective(
            gridMat,
            warped,
            transform,
            Size(warpedWidth.toDouble(), warpedHeight.toDouble()),
            Imgproc.INTER_CUBIC // Daha kaliteli interpolasyon
        )

        // Şimdi, düzlenmiş görüntüyü 9x9 eşit hücrelere bölüyoruz.
        val cells = mutableListOf<Bitmap>()
        // Her hücrenin orijinal boyutunu hesaplayın
        val cellWidth = warpedWidth.toDouble() / 9.0
        val cellHeight = warpedHeight.toDouble() / 9.0
        // Marj: Hücrenin kenarlarından ne kadar içeri alınacağı
        val marginX = cellWidth * marginRatio
        val marginY = cellHeight * marginRatio

        // Hücreleri sırayla ayırın
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                // Hücre ROI'si: marjları hesaba katarak kesin bölge
                val x = col * cellWidth + marginX
                val y = row * cellHeight + marginY
                val w = cellWidth - 2 * marginX
                val h = cellHeight - 2 * marginY

                // Sınırları integer yapın
                val roi = Rect(x.toInt(), y.toInt(), w.toInt(), h.toInt())
                val cellMat = Mat(warped, roi)

                // Eğer gerekirse, hücreyi hedef boyuta yeniden boyutlandırın
                val finalCellMat = Mat()
                Imgproc.resize(cellMat, finalCellMat, Size(cellSize.toDouble(), cellSize.toDouble()), 0.0, 0.0, Imgproc.INTER_AREA)

                // Mat'ten Bitmap'e dönüştürün
                val cellBitmap = Bitmap.createBitmap(cellSize, cellSize, Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(finalCellMat, cellBitmap)
                cells.add(cellBitmap)
            }
        }
        return@withContext cells
    }




    /**
     * Verilen sudoku ızgarasının dört köşesi ([tl, tr, br, bl]) üzerinden,
     * (row, col) indislerine göre interpolasyon yaparak o noktanın koordinatlarını hesaplar.
     *
     * Örneğin:
     * - row ve col 0 ise: sol üst köşe (tl)
     * - row ve col 9 ise: sağ alt köşe (br)
     */
    // **Interpolated Noktaları Daha Doğru Hesapla**
    private fun getInterpolatedPoint(
        corners: List<Point>,
        row: Int,
        col: Int,
        marginRatio: Double
    ): Point {
        val topLeft = corners[0]
        val topRight = corners[1]
        val bottomRight = corners[2]
        val bottomLeft = corners[3]

        val xFraction = (col / 9.0) + (marginRatio / 9.0)
        val yFraction = (row / 9.0) + (marginRatio / 9.0)

        val x = (1 - xFraction) * ((1 - yFraction) * topLeft.x + yFraction * bottomLeft.x) +
                xFraction * ((1 - yFraction) * topRight.x + yFraction * bottomRight.x)

        val y = (1 - yFraction) * ((1 - xFraction) * topLeft.y + xFraction * topRight.y) +
                yFraction * ((1 - xFraction) * bottomLeft.y + xFraction * bottomRight.y)

        return Point(x, y)
    }


    suspend fun extractSudokuCellsDirectly(
        originalBitmap: Bitmap,
        cellSize: Int = 28,
        marginRatio: Double = 0.03 // Daha küçük margin kullanıyoruz
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val sudokuCorners: List<Point> = detectSudokuCorners(originalBitmap)

        val originalMat = Mat()
        Utils.bitmapToMat(originalBitmap, originalMat)

        val cellBitmaps = mutableListOf<Bitmap>()

        for (row in 0 until 9) {
            for (col in 0 until 9) {
                val p1 = adjustPoint(getInterpolatedPoint(sudokuCorners, row, col), marginRatio)
                val p2 = adjustPoint(getInterpolatedPoint(sudokuCorners, row, col + 1), marginRatio)
                val p3 = adjustPoint(getInterpolatedPoint(sudokuCorners, row + 1, col + 1), marginRatio)
                val p4 = adjustPoint(getInterpolatedPoint(sudokuCorners, row + 1, col), marginRatio)

                val srcPoints = MatOfPoint2f(p1, p2, p3, p4)
                val dstPoints = MatOfPoint2f(
                    Point(0.0, 0.0),
                    Point(cellSize.toDouble(), 0.0),
                    Point(cellSize.toDouble(), cellSize.toDouble()),
                    Point(0.0, cellSize.toDouble())
                )

                val transformMatrix = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
                val cellMat = Mat()
                Imgproc.warpPerspective(
                    originalMat,
                    cellMat,
                    transformMatrix,
                    Size(cellSize.toDouble(), cellSize.toDouble()),
                    Imgproc.INTER_CUBIC
                )

                // **Merkezleme ve az boşluk bırakma fonksiyonunu çağır**
                val centeredMat = centerAndCropCell(cellMat, cellSize)

                // Bitmap'e çevirme
                val cellBitmap = Bitmap.createBitmap(cellSize, cellSize, Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(centeredMat, cellBitmap)
                cellBitmaps.add(cellBitmap)
            }
        }

        return@withContext cellBitmaps
    }

    /**
     * Hücreyi merkezde tutarak az boşluk bırakır.
     */
    private fun centerAndCropCell(cellMat: Mat, cellSize: Int): Mat {
        val gray = Mat()
        Imgproc.cvtColor(cellMat, gray, Imgproc.COLOR_BGR2GRAY)

        val thresh = Mat()
        Imgproc.threshold(gray, thresh, 50.0, 255.0, Imgproc.THRESH_BINARY_INV)

        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        if (contours.isNotEmpty()) {
            val boundingRect = Imgproc.boundingRect(contours[0])

            // **Görüntünün ne kadar dar olduğuna bağlı olarak padding belirle**
            val width = boundingRect.width.toDouble()
            val height = boundingRect.height.toDouble()

            // Eğer sayı çok darsa (örn: 1 ve 4), padding'i artır
            val paddingX = (width * 0.15).toInt().coerceAtLeast(5) // Minimum 5 piksel padding
            val paddingY = (height * 0.10).toInt().coerceAtLeast(5) // Minimum 5 piksel padding

            val startX = maxOf(0, boundingRect.x - paddingX)
            val startY = maxOf(0, boundingRect.y - paddingY)
            val endX = minOf(cellMat.cols(), boundingRect.x + boundingRect.width + paddingX)
            val endY = minOf(cellMat.rows(), boundingRect.y + boundingRect.height + paddingY)

            val croppedMat = Mat(cellMat, Rect(startX, startY, endX - startX, endY - startY))

            // **Son kareyi oluştur ve ortala**
            val newSize = Size(cellSize.toDouble(), cellSize.toDouble())
            val finalMat = Mat()
            Imgproc.resize(croppedMat, finalMat, newSize, 0.0, 0.0, Imgproc.INTER_CUBIC)

            return finalMat
        }

        return cellMat
    }



    /**
     * Sudoku ızgarasının 4 köşe noktasını kullanarak, istenen (row, col) indeksindeki noktayı bilineer
     * interpolasyon yöntemiyle hesaplar.
     *
     * Örnekte, sudoku ızgarası 9 hücreye bölündüğünden,
     * satır ve sütun indeksi 0'dan 9'a kadar (10 nokta) değerlendirilmektedir.
     *
     * @param corners Sudoku ızgarasının köşeleri [üst-sol, üst-sağ, alt-sağ, alt-sol].
     * @param row Satır indeksi (0..9).
     * @param col Sütun indeksi (0..9).
     */
    fun getInterpolatedPoint(corners: List<Point>, row: Int, col: Int): Point {
        // 9 hücre olduğundan, 10 çizgi noktası elde edilir.
        val gridSize = 9.0
        // Sütun için yatay interpolasyon oranı
        val alpha = col / gridSize
        // Satır için düşey interpolasyon oranı
        val beta = row / gridSize

        // Üst kenarda interpolasyon
        val top = Point(
            corners[0].x + (corners[1].x - corners[0].x) * alpha,
            corners[0].y + (corners[1].y - corners[0].y) * alpha
        )
        // Alt kenarda interpolasyon (alt-sol ile alt-sağ arasında)
        val bottom = Point(
            corners[3].x + (corners[2].x - corners[3].x) * alpha,
            corners[3].y + (corners[2].y - corners[3].y) * alpha
        )
        // Üst ve alt noktalar arasında düşey interpolasyon
        return Point(
            top.x + (bottom.x - top.x) * beta,
            top.y + (bottom.y - top.y) * beta
        )
    }

    /**
     * İsteğe bağlı olarak, marginRatio uygulanarak verilen noktayı hücre merkezine doğru hafifçe içeri alabilir.
     * Bu örnekte basitçe orijinal noktayı geri veriyoruz.
     *
     * @param point Orijinal hesaplanmış köşe noktası.
     * @param marginRatio Uygulanacak marj oranı.
     */
    fun adjustPoint(point: Point, marginRatio: Double): Point {
        // Bu fonksiyonda margin uygulaması yapabilirsiniz. Örneğin:
        // Noktayı hücre merkezine doğru belirli oranda içeri almak için hesaplama yapabilirsiniz.
        // Şimdilik, orijinal noktayı geri döndürüyoruz.
        return point
    }
    fun removeBorders(bitmap: Bitmap, threshold: Int = 10): Bitmap {
        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)

        // 📌 Gri tonlamaya çevir
        val gray = Mat()
        Imgproc.cvtColor(srcMat, gray, Imgproc.COLOR_BGR2GRAY)

        // 📌 Kenarları tespit et
        val edges = Mat()
        Imgproc.Canny(gray, edges, 50.0, 150.0)

        // 📌 Konturları bul
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        var xMin = srcMat.cols()
        var xMax = 0
        var yMin = srcMat.rows()
        var yMax = 0

        for (contour in contours) {
            val rect = Imgproc.boundingRect(contour)

            // 📌 Görüntü sınırlarını kontrol et (Mat boyutlarını aşmamak için)
            xMin = maxOf(0, minOf(xMin, rect.x))
            yMin = maxOf(0, minOf(yMin, rect.y))
            xMax = minOf(srcMat.cols(), maxOf(xMax, rect.x + rect.width))
            yMax = minOf(srcMat.rows(), maxOf(yMax, rect.y + rect.height))
        }

        // 📌 Kenarları biraz daha kırp (Threshold kullanarak içeri al)
        xMin = maxOf(0, xMin + threshold)
        yMin = maxOf(0, yMin + threshold)
        xMax = minOf(srcMat.cols(), xMax - threshold)
        yMax = minOf(srcMat.rows(), yMax - threshold)

        // 📌 Eğer kırpma boyutu hatalıysa orijinal görseli döndür
        if (xMin >= xMax || yMin >= yMax) {
            return bitmap
        }

        // 📌 Yeni kırpılmış görüntü
        val croppedMat = srcMat.submat(yMin, yMax, xMin, xMax)

        // 📌 Bitmap'e çevir
        val croppedBitmap = Bitmap.createBitmap(croppedMat.cols(), croppedMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(croppedMat, croppedBitmap)

        return croppedBitmap
    }
    fun isImageBlurry(bitmap: Bitmap, threshold: Double = 100.0): Boolean {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

        // Bitmap'i OpenCV Mat formatına çevir
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Gri tonlamaya çevir
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY)

        // Laplacian filtresi uygula
        val laplacian = Mat()
        Imgproc.Laplacian(mat, laplacian, CvType.CV_64F)

        // Varyans hesapla
        val meanStdDev = MatOfDouble()
        val stdDev = MatOfDouble()
        Core.meanStdDev(laplacian, meanStdDev, stdDev)

        val variance = stdDev.toArray()[0] * stdDev.toArray()[0]

        return variance < threshold // Varyans düşükse bulanık sayılır
    }
    fun sharpenBitmap(bitmap: Bitmap): Bitmap {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

        // Bitmap'i Mat formatına çevir
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Gauss bulanıklaştırma uygula
        val blurred = Mat()
        Imgproc.GaussianBlur(mat, blurred, Size(0.0, 0.0), 3.0)

        // Keskinleştirme (sharpening) için orijinalden bulanık çıkart
        val sharpened = Mat()
        Core.addWeighted(mat, 1.5, blurred, -0.5, 0.0, sharpened)

        // Mat'i tekrar Bitmap'e çevir
        val outputBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        Utils.matToBitmap(sharpened, outputBitmap)

        return outputBitmap
    }

}
