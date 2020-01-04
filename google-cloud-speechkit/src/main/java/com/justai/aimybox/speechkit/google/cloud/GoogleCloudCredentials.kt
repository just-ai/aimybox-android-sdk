package com.justai.aimybox.speechkit.google.cloud

import android.content.Context
import androidx.annotation.RawRes
import com.google.auth.Credentials
import com.google.auth.oauth2.ServiceAccountCredentials
import java.io.InputStream

class GoogleCloudCredentials private constructor(internal val credentials: Credentials) {
    companion object {
        fun fromAsset(context: Context, assetFile: String) = GoogleCloudCredentials(
            ServiceAccountCredentials.fromStream(context.assets.open(assetFile).buffered())
        )

        fun fromRawResource(context: Context, @RawRes resource: Int) = GoogleCloudCredentials(
            ServiceAccountCredentials.fromStream(context.resources.openRawResource(resource))
        )

        fun fromInputStream(inputStream: InputStream) = GoogleCloudCredentials(
            ServiceAccountCredentials.fromStream(inputStream)
        )
    }
}