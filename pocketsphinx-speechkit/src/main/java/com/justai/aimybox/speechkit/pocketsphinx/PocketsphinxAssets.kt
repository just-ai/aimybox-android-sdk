package com.justai.aimybox.speechkit.pocketsphinx

import android.content.Context
import android.os.Environment
import androidx.annotation.RequiresPermission
import java.io.File
import java.io.InputStream

class PocketsphinxAssets private constructor(
    val acousticModelFilePath: String,
    val dictionaryFilePath: String,
    val grammarFilePath: String
){
    companion object {
        @RequiresPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        fun fromExternalStoragePath(
            directory: String,
            acousticModelFileName: String,
            dictionaryFileName: String,
            grammarFileName: String
        ): PocketsphinxAssets {
            L.i("Reading assets from external storage. Full path: $directory")
            return PocketsphinxAssets(
                directory + acousticModelFileName,
                directory + dictionaryFileName,
                directory + grammarFileName)
        }

        @RequiresPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        fun fromApkAssets(
            context: Context,
            acousticModelFileName: String,
            dictionaryFileName: String,
            grammarFileName: String,
            assetsDirectory: String = "",
            externalStorageDirectory: String = "Android/data/pocketsphinx-assets/"
        ): PocketsphinxAssets {
            val directory =
                "${Environment.getExternalStorageDirectory().absolutePath}/$externalStorageDirectory"

            File(directory).takeIf { it.exists() }?.deleteRecursively()
            copyAssetToExternalStorage(context, assetsDirectory, acousticModelFileName, directory)
            copyAssetToExternalStorage(context, assetsDirectory, dictionaryFileName, directory)
            copyAssetToExternalStorage(context, assetsDirectory, grammarFileName, directory)

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