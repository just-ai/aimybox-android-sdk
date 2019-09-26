package com.justai.aimybox.speechkit.snowboy

import android.content.Context
import android.os.Environment
import androidx.annotation.RequiresPermission
import java.io.File


class SnowboyAssets private constructor(val modelFilePath: String, val resourcesFilePath: String) {
    companion object {
        @RequiresPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        fun fromExternalStoragePath(
            directory: String,
            modelFileName: String,
            resourcesFileName: String
        ): SnowboyAssets {
            L.i("Reading assets from external storage. Full path: $directory")
            return SnowboyAssets(directory + modelFileName, directory + resourcesFileName)
        }

        @RequiresPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        fun fromApkAssets(
            context: Context,
            modelFileName: String,
            resourcesFileName: String,
            assetsDirectory: String = "",
            externalStorageDirectory: String = "Android/data/snowboy-assets/"
        ): SnowboyAssets {
            val directory =
                "${Environment.getExternalStorageDirectory().absolutePath}/$externalStorageDirectory"

            if (!File(directory + modelFileName).exists() || !File(directory + resourcesFileName).exists()) {
                L.i("Asset files is not present on external storage. Copying...")
                File(directory).mkdirs()
                copyAssetToExternalStorage(context, assetsDirectory, modelFileName, directory)
                copyAssetToExternalStorage(context, assetsDirectory, resourcesFileName, directory)
            } else {
                L.i("Asset files is already present on external storage directory.")
            }

            return fromExternalStoragePath(directory, modelFileName, resourcesFileName)
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

            try {
                assetManager.open(filePath).use { inputStream ->
                    File(destination + filename)
                        .apply {
                            createNewFile()
                        }
                        .outputStream()
                        .use { outputStream ->
                            inputStream.copyTo(outputStream, 1024)
                            outputStream.flush()
                        }
                }
            } catch (e: Throwable) {
                L.e("Failed to copy $filename from assets to SD", e)
            }
        }
    }
}