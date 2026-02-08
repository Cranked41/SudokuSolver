package com.cranked.sudokusolver.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

object BitmapUtil {
    fun overlaySolutionOnGrid(
        gridBitmap: Bitmap,
        original: Array<IntArray>,
        solved: Array<IntArray>
    ): Bitmap {
        val out = gridBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)

        val cellW = out.width / 9f
        val cellH = out.height / 9f

        val givenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            textSize = cellH * 0.70f
        }

        val solvedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 120, 255) // çözülen rakamlar mavi
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            textSize = cellH * 0.70f
        }

        // Dikey merkezleme için baseline hesapla
        fun baselineForRow(r: Int, paint: Paint): Float {
            val yTop = r * cellH
            val yCenter = yTop + cellH / 2f
            val fm = paint.fontMetrics
            return yCenter - (fm.ascent + fm.descent) / 2f
        }

        for (r in 0 until 9) {
            for (c in 0 until 9) {
                val v = solved[r][c]
                if (v == 0) continue

                val xCenter = c * cellW + cellW / 2f
                val paint = if (original[r][c] == 0) solvedPaint else givenPaint
                val yBase = baselineForRow(r, paint)

                canvas.drawText(v.toString(), xCenter, yBase, paint)
            }
        }

        return out
    }
}