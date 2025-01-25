package com.cranked.sudokusolver.utils.file_utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object FileUtil {
     fun copyFile(sourceFile: File, targetFile: String): Boolean {
        return try {
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                val fileReader = ByteArray(4096)
                val fileSize = FileInputStream(sourceFile)
                var fileSizeDownloaded: Long = 0
                inputStream = fileSize
                outputStream = FileOutputStream(targetFile)
                while (true) {
                    val read = inputStream.read(fileReader)
                    if (read == -1) {
                        break
                    }
                    outputStream.write(fileReader, 0, read)
                    fileSizeDownloaded += read.toLong()
                }
                outputStream.flush()
                true
            } catch (e: IOException) {

                false
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: IOException) {

            e.printStackTrace()
            false
        }
    }
}