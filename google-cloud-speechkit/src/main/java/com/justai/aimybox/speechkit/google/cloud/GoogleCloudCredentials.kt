package com.justai.aimybox.speechkit.google.cloud

import android.content.Context
import android.system.Os
import androidx.annotation.RawRes
import com.justai.aimybox.extensions.writeToInternalStorageFile
import java.io.File

object GoogleCloudCredentials {
    private const val PROPERTY = "GOOGLE_APPLICATION_CREDENTIALS"
    private const val FILENAME = "google-cloud-credentials.json"

    fun loadFromRawRes(context: Context, @RawRes credentialsRes: Int) {
        context.resources.openRawResource(credentialsRes)
            .writeToInternalStorageFile(context, FILENAME)
            ?.let(::loadFromFile)
    }

    fun loadFromAsset(context: Context, file: String) {
        context.assets.open(file)
            .writeToInternalStorageFile(context, FILENAME)
            ?.let(::loadFromFile)
    }

    fun loadFromFile(file: File) {
        Os.setenv(PROPERTY, file.absolutePath, true)
    }
}