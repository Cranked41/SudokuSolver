package com.cranked.sudokusolver.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.googlecode.leptonica.android.Binarize
import com.googlecode.leptonica.android.Convert
import com.googlecode.leptonica.android.ReadFile
import com.googlecode.leptonica.android.Rotate
import com.googlecode.leptonica.android.Scale
import com.googlecode.leptonica.android.Skew
import com.googlecode.leptonica.android.WriteFile
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

fun Bitmap.preProcessBitmap(): Bitmap {
    var bitmap = this
    bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    var pix = ReadFile.readBitmap(bitmap)
    pix = Convert.convertTo8(pix)
    pix = Scale.scale(pix, 1.5f) //..
    // pix = AdaptiveMap.pixContrastNorm(pix)
    //pix = Enhance.unsharpMasking(pix)
    pix = Binarize.otsuAdaptiveThreshold(pix)
    val f = Skew.findSkew(pix)
    pix = Rotate.rotate(pix, f)
    return WriteFile.writeBitmap(pix)
}

fun Bitmap.processForOCR(): Bitmap {
    // OpenCV Mat'e dönüştür
    val src = Mat()
    Utils.bitmapToMat(this, src)

    // Gri tonlamaya çevir
    val gray = Mat()
    Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)

    // Uyarlanabilir eşikleme uygula
    val thresholded = Mat()
    Imgproc.adaptiveThreshold(
        gray,
        thresholded,
        255.0, // Maksimum piksel değeri
        Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, // Gaussian Threshold
        Imgproc.THRESH_BINARY, // İkili eşikleme
        15, // Blok boyutu
        4.0 // Sabit C değeri
    )

    // Gürültü azaltma
    //Imgproc.GaussianBlur(thresholded, thresholded, Size(5.0, 5.0), 0.0)

    // Ters eşikleme (isteğe bağlı)
    Core.bitwise_not(thresholded, thresholded)

    // Sonuç görüntüsünü Bitmap'e dönüştür
    val resultBitmap =
        Bitmap.createBitmap(thresholded.cols(), thresholded.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(thresholded, resultBitmap)

    // Kaynakları serbest bırak
    src.release()
    gray.release()
    thresholded.release()

    return resultBitmap
}
fun Bitmap.toGrayscale(): Bitmap {
    val width = this.width
    val height = this.height
    val grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(grayBitmap)
    val paint = Paint()
    val colorMatrix = ColorMatrix()
    colorMatrix.setSaturation(0f)
    val colorMatrixFilter = ColorMatrixColorFilter(colorMatrix)
    paint.colorFilter = colorMatrixFilter
    canvas.drawBitmap(this, 0f, 0f, paint)
    return grayBitmap
}
fun Bitmap.threshold(threshold: Int): Bitmap {
    val width = this.width
    val height = this.height
    val thresholdBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    for (x in 0 until width) {
        for (y in 0 until height) {
            val pixel = this.getPixel(x, y)
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)
            val gray = (red + green + blue) / 3
            val newPixel = if (gray < threshold) Color.BLACK else Color.WHITE
            thresholdBitmap.setPixel(x, y, newPixel)
        }
    }

    return thresholdBitmap
}
fun Bitmap.applyThreshold(): Bitmap {
    val mat = Mat()
    Utils.bitmapToMat(this, mat)

    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY) // Grayscale'e çevir
    Imgproc.GaussianBlur(mat, mat, Size(5.0, 5.0), 0.0) // Gaussian Blur uygula
    Imgproc.threshold(
        mat,
        mat,
        0.0,
        255.0,
        Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU
    ) // Otsu Threshold uygula

    val resultBitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(mat, resultBitmap)
    return resultBitmap
}
fun Bitmap.reduceNoise(): Bitmap {
    // Bu işlem için OpenCV gibi bir kütüphane kullanabilirsiniz.
    // Bu örnekte basit bir median blur uygulaması gösterilmektedir.
    val blurredBitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(blurredBitmap)
    val paint = Paint()
    paint.isAntiAlias = true
    paint.isFilterBitmap = true
    canvas.drawBitmap(this, 0f, 0f, paint)
    return blurredBitmap
}
fun Bitmap.increaseContrast(contrast: Float): Bitmap {
    val width = this.width
    val height = this.height
    val contrastBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(contrastBitmap)
    val paint = Paint()
    val colorMatrix = ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, 0f,
            0f, contrast, 0f, 0f, 0f,
            0f, 0f, contrast, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(this, 0f, 0f, paint)
    return contrastBitmap
}
fun Bitmap.resizeBitmap( width: Int, height: Int): Bitmap {
    return Bitmap.createScaledBitmap(this, width, height, true)
}
fun Bitmap.convertToGrayscale(): Bitmap {
    val grayscaleBitmap =
        Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(grayscaleBitmap)
    val paint = Paint()
    val matrix = ColorMatrix()
    matrix.setSaturation(0f) // Gri tonlama
    val filter = ColorMatrixColorFilter(matrix)
    paint.setColorFilter(filter)
    canvas.drawBitmap(this, 0f, 0f, paint)
    return grayscaleBitmap
}

fun Bitmap.applyThreshold(threshold: Int): Bitmap {
    val thresholdBitmap =
        Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)

    for (x in 0 until this.width) {
        for (y in 0 until this.height) {
            val pixel = this.getPixel(x, y)
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)
            val gray = (red + green + blue) / 3

            if (gray > threshold) {
                thresholdBitmap.setPixel(x, y, Color.WHITE) // Beyaz
            } else {
                thresholdBitmap.setPixel(x, y, Color.BLACK) // Siyah
            }
        }
    }

    return thresholdBitmap
}

fun Bitmap.removeNoise(): Bitmap {
    // OpenCV'yi kullanarak gürültü temizleme
    val mat = Mat()
    Utils.bitmapToMat(this, mat)

    // Morfolojik işlemler için kernel
    val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))

    // Gürültüyü azaltmak için açma işlemi (erozyon + genişleme)
    Imgproc.morphologyEx(mat, mat, Imgproc.MORPH_OPEN, kernel)

    val cleanedBitmap =
        Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(mat, cleanedBitmap)

    return cleanedBitmap
}

fun Bitmap.preprocessImage(): Bitmap {
    // Robust OCR pre-processing (especially for digits / high-contrast symbols)
    // Pipeline: grayscale -> blur -> adaptive threshold -> morph open/close -> crop to content
    // -> normalize (keep aspect) -> pad to 64x64 -> ensure black text on white bg

    val src = Mat()
    Utils.bitmapToMat(this, src)

    // Ensure we work in grayscale
    val gray = Mat()
    if (src.channels() == 1) {
        src.copyTo(gray)
    } else {
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
    }

    // Noise suppression
    val blurred = Mat()
    Imgproc.GaussianBlur(gray, blurred, Size(3.0, 3.0), 0.0)

    // Adaptive threshold is more robust than a fixed threshold
    val bin = Mat()
    Imgproc.adaptiveThreshold(
        blurred,
        bin,
        255.0,
        Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
        Imgproc.THRESH_BINARY,
        31, // block size (odd). Increase if illumination varies.
        7.0 // C value. Increase if strokes become too thick.
    )

    // Morphological cleanup
    val kOpen = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(2.0, 2.0))
    val kClose = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
    Imgproc.morphologyEx(bin, bin, Imgproc.MORPH_OPEN, kOpen)
    Imgproc.morphologyEx(bin, bin, Imgproc.MORPH_CLOSE, kClose)

    // Crop to content
    val content = bin.cropToContent(whiteThreshold = 245)

    // Ensure black foreground on white background
    if (content.shouldInvertForOCR()) {
        Core.bitwise_not(content, content)
    }

    // Normalize: keep aspect then pad to 64x64
    val normalized = content.resizeKeepAspect(maxSide = 56)
        .padToSquare(squareSize = 64, padValue = 255)

    val out = Bitmap.createBitmap(normalized.cols(), normalized.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(normalized, out)

    // Release
    src.release()
    gray.release()
    blurred.release()
    bin.release()
    content.release()
    normalized.release()

    return out
}

private fun Mat.shouldInvertForOCR(): Boolean {
    // If the image is mostly dark, invert to keep black text on white background.
    val mean = Core.mean(this).`val`[0]
    return mean < 127.0
}

private fun Mat.cropToContent(whiteThreshold: Int = 245): Mat {
    // Works on a single-channel binary-ish image where background is near-white.
    // Find bounding box of pixels below `whiteThreshold`.
    val mask = Mat()
    Imgproc.threshold(this, mask, whiteThreshold.toDouble(), 255.0, Imgproc.THRESH_BINARY_INV)

    val nonZero = Mat()
    Core.findNonZero(mask, nonZero)

    if (nonZero.empty()) {
        mask.release()
        nonZero.release()
        return this.clone()
    }

    val rect = Imgproc.boundingRect(nonZero)

    val x = rect.x.coerceAtLeast(0)
    val y = rect.y.coerceAtLeast(0)
    val w = rect.width.coerceAtMost(this.cols() - x)
    val h = rect.height.coerceAtMost(this.rows() - y)

    // Add a small margin to avoid cutting strokes
    val pad = 2
    val x2 = (x - pad).coerceAtLeast(0)
    val y2 = (y - pad).coerceAtLeast(0)
    val x3 = (x + w + pad).coerceAtMost(this.cols())
    val y3 = (y + h + pad).coerceAtMost(this.rows())

    val cropped = this.submat(y2, y3, x2, x3).clone()

    mask.release()
    nonZero.release()

    return cropped
}

private fun Mat.resizeKeepAspect(maxSide: Int): Mat {
    val w = this.cols()
    val h = this.rows()
    if (w <= 0 || h <= 0) return this.clone()

    val scale = if (w >= h) maxSide.toDouble() / w.toDouble() else maxSide.toDouble() / h.toDouble()
    val newW = (w * scale).toInt().coerceAtLeast(1)
    val newH = (h * scale).toInt().coerceAtLeast(1)

    val resized = Mat()
    Imgproc.resize(this, resized, Size(newW.toDouble(), newH.toDouble()), 0.0, 0.0, Imgproc.INTER_AREA)
    return resized
}

private fun Mat.padToSquare(squareSize: Int, padValue: Int): Mat {
    val w = this.cols()
    val h = this.rows()

    val out = Mat(squareSize, squareSize, this.type())
    out.setTo(org.opencv.core.Scalar(padValue.toDouble()))

    val x = ((squareSize - w) / 2).coerceAtLeast(0)
    val y = ((squareSize - h) / 2).coerceAtLeast(0)

    val roiW = w.coerceAtMost(squareSize)
    val roiH = h.coerceAtMost(squareSize)

    val roi = out.submat(y, y + roiH, x, x + roiW)
    val srcRoi = this.submat(0, roiH, 0, roiW)
    srcRoi.copyTo(roi)

    roi.release()
    srcRoi.release()

    return out
}

fun Bitmap.cropBitmap(threshold: Int = 250): Bitmap {
    val width = this.width
    val height = this.height
    val pixels = IntArray(width * height)
    this.getPixels(pixels, 0, width, 0, 0, width, height)

    var left = width
    var right = 0
    var top = height
    var bottom = 0

    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel = pixels[y * width + x]
            val alpha = (pixel shr 24) and 0xff
            val red = (pixel shr 16) and 0xff
            val green = (pixel shr 8) and 0xff
            val blue = pixel and 0xff

            // Eğer pixel beyaz değilse (threshold altında) kenarları güncelle
            if (alpha > 0 && red < threshold && green < threshold && blue < threshold) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
    }

    // Eğer tamamen boşsa, orijinal bitmap'i döndür
    if (left >= right || top >= bottom) return this

    return Bitmap.createBitmap(this, left, top, right - left + 1, bottom - top + 1)
}

fun Bitmap.cropLeftRight(threshold: Int = 250): Bitmap {
    val width = this.width
    val height = this.height
    val pixels = IntArray(width * height)
    this.getPixels(pixels, 0, width, 0, 0, width, height)

    var left = width
    var right = 0

    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel = pixels[y * width + x]
            val alpha = (pixel shr 24) and 0xff
            val red = (pixel shr 16) and 0xff
            val green = (pixel shr 8) and 0xff
            val blue = pixel and 0xff

            // Eğer pixel beyaz değilse (threshold altında) kenarları güncelle
            if (alpha > 0 && red < threshold && green < threshold && blue < threshold) {
                if (x < left) left = x
                if (x > right) right = x
            }
        }
    }

    // Eğer tamamen boşsa, orijinal bitmap'i döndür
    if (left >= right) return this

    return Bitmap.createBitmap(this, left, 0, right - left + 1, height)
}

fun Bitmap.cropAndMakeOval(): Bitmap {
    // Yeni oval bitmap oluştur
    val output = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    // Anti-aliasing ile düzgün kenarlar çizmek için Paint oluştur
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.WHITE

    // Oval maske oluştur
    val rect = Rect(0, 0, this.width, this.height)
    val rectF = RectF(rect)

    // Yuvarlatılmış dikdörtgen çiz
    canvas.drawOval(rectF, paint)

    // Orijinal bitmap'i oval maskeye uygula
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(this, rect, rect, paint)

    return output
}

fun Bitmap.cropAndRoundCorners(radius: Float = 20f): Bitmap {
    // Yeni yuvarlatılmış bitmap oluştur
    val output = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    // Yuvarlatılmış dikdörtgen çizmek için Paint oluştur
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.WHITE

    // Yuvarlatılmış dikdörtgen maskesi oluştur
    val rect = Rect(0, 0, this.width, this.height)
    val rectF = RectF(rect)

    // Köşeleri belirli bir radius ile yuvarlatılmış dikdörtgen çiz
    canvas.drawRoundRect(rectF, radius, radius, paint)

    // Orijinal bitmap'i yuvarlatılmış maskeye uygula
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(this, rect, rect, paint)

    return output
}

fun Bitmap.toBlackAndWhite(threshold: Int = 128): Bitmap {
    val bmpBlackWhite = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmpBlackWhite)
    val paint = Paint()

    val colorMatrix = ColorMatrix(
        floatArrayOf(
            1f, 1f, 1f, 0f, -255f,
            1f, 1f, 1f, 0f, -255f,
            1f, 1f, 1f, 0f, -255f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    val filter = ColorMatrixColorFilter(colorMatrix)
    paint.colorFilter = filter
    canvas.drawBitmap(this, 0f, 0f, paint)

    return bmpBlackWhite
}


