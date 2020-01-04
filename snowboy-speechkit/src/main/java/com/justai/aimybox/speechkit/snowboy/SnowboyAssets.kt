package com.justai.aimybox.speechkit.snowboy

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.RequiresPermission
import com.justai.aimybox.extensions.writeToInternalStorageFile
import java.io.File


class SnowboyAssets private constructor(val modelFilePath: String, val resourcesFilePath: String) {
    companion object {
        @RequiresPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        fun fromFile(
            directory: String,
            modelFileName: String,
            resourcesFileName: String
        ): SnowboyAssets {
            L.i("Reading assets from storage. Full path: $directory")
            return SnowboyAssets(directory + modelFileName, directory + resourcesFileName)
        }

        @SuppressLint("MissingPermission")
        fun fromAssets(
            context: Context,
            modelFile: String,
            resourcesFile: String
        ): SnowboyAssets {
            val cacheDir = context.cacheDir.absolutePath

            val cachedResources = cacheDir + resourcesFile
            val cachedModel = cacheDir + modelFile

            if (!File(cachedModel).exists()) {
                L.i("Copying model from assets to the cache")
                context.assets.open(modelFile).writeToInternalStorageFile(context, modelFile)
            } else {
                L.i("Model is found in the cache")
            }

            if (!File(cachedResources).exists()) {
                L.i("Copying resources from assets to the cache")
                context.assets.open(resourcesFile).writeToInternalStorageFile(context, resourcesFile)
            } else {
                L.i("Resources is found in the cache")
            }

            return fromFile(cacheDir, modelFile, resourcesFile)
        }
    }
}
