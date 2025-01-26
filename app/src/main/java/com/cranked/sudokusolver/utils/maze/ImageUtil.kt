package com.cranked.sudokusolver.utils.maze

import android.graphics.*

object ImageUtil {

    fun rotateImage(bitmap: Bitmap, rotationDegrees: Float = 90f): Bitmap {
        val matrixR = Matrix()
        matrixR.postRotate(rotationDegrees)

        val scaledBitmap: Bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, true)

        return Bitmap.createBitmap(
            scaledBitmap,
            0,
            0,
            scaledBitmap.width,
            scaledBitmap.height,
            matrixR,
            true
        )
    }

    //CropRect -> Firebase face detection.
    //CropRectF -> TensorFlow.
    fun getCropBitmap(source: Bitmap, cropRect: Rect?): Bitmap? {
        if (cropRect == null) return null
        val cropRectWidth = cropRect.width()
        val cropRectHeight = cropRect.height()
        val cropRectTop= -cropRect.top.toFloat()
        val cropRectLeft = -cropRect.left.toFloat()

        val topH = 150
        val topY = 50

        val resultBitmap = Bitmap.createBitmap(
            cropRectWidth, cropRectHeight + topH,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(resultBitmap)

        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        paint.color = Color.WHITE
        canvas.drawRect(
            Rect(0, topH, cropRectWidth, cropRectHeight),
            paint
        )

        val matrix = Matrix()
        matrix.postTranslate(cropRectLeft,cropRectTop + topY)
        canvas.drawBitmap(source, matrix, paint)
        return resultBitmap
    }

}