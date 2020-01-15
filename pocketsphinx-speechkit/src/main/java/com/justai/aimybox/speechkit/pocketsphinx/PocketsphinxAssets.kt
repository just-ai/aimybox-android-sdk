package com.justai.aimybox.speechkit.pocketsphinx

import android.annotation.SuppressLint
import android.content.Context
import java.io.File
import java.io.InputStream

class PocketsphinxAssets private constructor(
    val acousticModelFilePath: String,
    val dictionaryFilePath: String,
    val grammarFilePath: String? = null
){
    companion object {
        fun fromExternalStoragePath(
            directory: String,
            acousticModelFileName: String,
            dictionaryFileName: String,
            grammarFileName: String? = null
        ): PocketsphinxAssets {
            L.i("Reading assets from external storage. Full path: $directory")
            return PocketsphinxAssets(
                directory + acousticModelFileName,
                directory + dictionaryFileName,
                grammarFileName?.let { directory + grammarFileName })
        }

        @SuppressLint("NewApi")
        fun fromApkAssets(
            context: Context,
            acousticModelFileName: String,
            dictionaryFileName: String,
            grammarFileName: String? = null,
            assetsDirectory: String = ""
        ): PocketsphinxAssets {
            val directory =
                "${context.getExternalFilesDir(null)?.absolutePath}/pocketsphinx-assets/"

            if (!File(directory).exists()) {
                copyAssetToExternalStorage(context, assetsDirectory, acousticModelFileName, directory)
                copyAssetToExternalStorage(context, assetsDirectory, dictionaryFileName, directory)
                grammarFileName?.also {
                    copyAssetToExternalStorage(context, assetsDirectory, grammarFileName, directory)
                }
            }

            return fromExternalStoragePath(directory, acousticModelFileName, dictionaryFileName, grammarFileName)
        }

        private fun copyAssetToExternalStorage(
            context: Context,
            assetsDirectory: String,
            filename: String,
            destination: String
        ) {
            val assetManager = context.assets

            val filePath = if (assetsDirectory.isNotBlank()) {
                "$assetsDirectory/$filename"
            } else {
                filename
            }

            val files = assetManager.list(filePath)
            if (files?.isNotEmpty() == true) {
                files.forEach {
                    try {
                        copyAssetFileToExternalStorage(assetManager.open("$filePath/$it"),
                            "$destination$filename/$it")
                    } catch (e: Throwable) {
                        L.e("Cannot copy $destination$filename/$it", e)
                    }
                }
            } else {
                copyAssetFileToExternalStorage(assetManager.open(filePath), destination + filename)
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