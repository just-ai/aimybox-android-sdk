package com.justai.aimybox.extensions

import android.content.Context
import android.util.Log
import java.io.File
import java.io.InputStream

fun InputStream.writeToInternalStorageFile(context: Context, outputFile: String) = try {
    use { inputStream ->
        File(context.filesDir.absolutePath + outputFile).apply {
            createNewFile()
            outputStream().use { outputStream ->
                inputStream.copyTo(outputStream, 1024)
                outputStream.flush()
            }
        }
    }
} catch (e: Exception) {
    Log.e("Cache", "Failed to write $outputFile", e)
    null
}