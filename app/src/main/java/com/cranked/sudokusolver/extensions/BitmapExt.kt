package com.cranked.sudokusolver.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
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
    pix = Scale.scale(pix, 2f) //..
    // pix = AdaptiveMap.pixContrastNorm(pix)
    // pix = Enhance.unsharpMasking(pix)
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