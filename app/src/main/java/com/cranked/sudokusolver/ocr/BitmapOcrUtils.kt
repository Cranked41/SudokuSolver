package com.cranked.sudokusolver.ocr

import com.googlecode.tesseract.android.TessBaseAPI

import android.graphics.Bitmap
import com.googlecode.leptonica.android.AdaptiveMap
import com.googlecode.leptonica.android.Binarize
import com.googlecode.leptonica.android.Convert
import com.googlecode.leptonica.android.Enhance
import com.googlecode.leptonica.android.ReadFile
import com.googlecode.leptonica.android.Rotate
import com.googlecode.leptonica.android.Skew
import com.googlecode.leptonica.android.WriteFile

object BitmapOcrUtils {
    fun preProcessBitmap(tBitmap: Bitmap): Bitmap? {
        var bitmap = tBitmap
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        var pix = ReadFile.readBitmap(bitmap)
        pix = Convert.convertTo8(pix)
        pix = AdaptiveMap.pixContrastNorm(pix)
        pix = Enhance.unsharpMasking(pix)
        pix = Binarize.otsuAdaptiveThreshold(pix)
        val f = Skew.findSkew(pix)
        pix = Rotate.rotate(pix, f)
        return WriteFile.writeBitmap(pix)
    }
}