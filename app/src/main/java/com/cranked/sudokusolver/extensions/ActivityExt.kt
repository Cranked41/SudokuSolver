package com.cranked.sudokusolver.extensions

import android.app.Activity
import android.widget.Toast
import com.cranked.sudokusolver.utils.CoroutineCustomExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun Activity.getAssetAsFile(assetFileName: String): File {
    val tempFile = File(cacheDir, assetFileName)
    assets.open(assetFileName).use { inputStream ->
        FileOutputStream(tempFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
    return tempFile
}

fun Activity.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    CoroutineScope(Dispatchers.Main + CoroutineCustomExceptionHandler.handler).launch {
        Toast.makeText(this@showToast, message, duration).show()
    }
}

fun Activity.copyAssetToInternalStorage(assetFileName: String, targetDirName: String): File {
    // Hedef dizini oluştur
    val targetDir = File(filesDir, targetDirName)
    if (!targetDir.exists()) {
        targetDir.mkdirs()
    }

    // Hedef dosya yolu
    val targetFile = File(targetDir, assetFileName)

    // Dosyayı assets'ten kopyala
    assets.open(assetFileName).use { inputStream ->
        FileOutputStream(targetFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }

    return targetFile
}
fun CharSequence?.isNotNullOrEmptyOrBlank(): Boolean =
    this.isNullOrEmpty().not() && this?.isBlank()?.not() == true && this != "null"

fun CharSequence?.isNullOrEmptyOrBlank(): Boolean =
    this.isNullOrEmpty() || this?.isBlank() == true || this == "null"

