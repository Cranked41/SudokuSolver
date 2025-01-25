package com.cranked.sudokusolver.extensions

import android.app.Activity
import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun Context.copyAssetToSpecificDir(assetFileName: String, dirName: String): File? {
    try {
        // Hedef dizini oluştur: /data/data/com.example.app/files/<dirName>
        val targetDir = File(filesDir, dirName)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        // Hedef dosya yolu
        val targetFile = File(targetDir, assetFileName)

        // Eğer dosya zaten varsa yeniden kopyalamadan dönebilirsiniz
        if (targetFile.exists()) {
            println("Dosya zaten mevcut: ${targetFile.absolutePath}")
            return targetFile
        }

        // Dosyayı assets'ten kopyala
        assets.open(assetFileName).use { inputStream ->
            FileOutputStream(targetFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        println("Dosya başarıyla kopyalandı: ${targetFile.absolutePath}")
        return targetFile
    } catch (e: IOException) {
        e.printStackTrace()
        println("Dosya kopyalanamadı: $assetFileName")
    }
    return null
}