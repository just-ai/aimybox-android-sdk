package com.justai.aimybox.speechkit.kaldi

import android.annotation.SuppressLint
import android.content.Context
import java.io.File
import java.io.InputStream

class KaldiAssets private constructor(
    val directory: String
){
    companion object {

        @SuppressLint("NewApi")
        fun fromApkAssets(
            context: Context,
            assetsDirectory: String
        ): KaldiAssets {
            val directory =
                "${context.getExternalFilesDir(null)?.absolutePath}/kaldi-assets/"

            File(directory).takeIf { !it.exists() }?.also {
                copyAssetToExternalStorage(context, assetsDirectory, directory)
            }

            return KaldiAssets(directory)
        }

        private fun copyAssetToExternalStorage(
            context: Context,
            assetsDirectory: String,
            destination: String
        ) {
            val assetManager = context.assets
            val files = assetManager.list(assetsDirectory)

            files?.forEach {file ->
                try {
                    if (assetManager.list("$assetsDirectory/$file")!!.isNotEmpty()) {
                        copyAssetToExternalStorage(context, "$assetsDirectory/$file", "$destination/$file")
                    } else {
                        copyAssetFileToExternalStorage(assetManager.open("$assetsDirectory/$file"),
                            "$destination/$file")
                    }
                } catch (e: Throwable) {
                    L.e("Cannot copy $destination/$file", e)
                }
            }
        }

        private fun copyAssetFileToExternalStorage(
            stream: InputStream,
            destinationPath: String) {

            stream.use { inputStream ->
                File(destinationPath)
                    .apply {
                        parentFile?.mkdirs()
                        createNewFile()
                    }
                    .outputStream()
                    .use { outputStream ->
                        inputStream.copyTo(outputStream, 1024)
                        outputStream.flush()
                    }
            }
        }
    }
}