# Google Cloud Speechkit for Aimybox Android SDK

Speech recognition and synthesis by [Google Cloud](https://cloud.google.com/products/)

## How to start using

1. Login into [Google Cloud Console](https://console.cloud.google.com)

2. Select or create a project

3. Enable Speech-To-Text and/or Text-To-Speech API

4. Go to [Credentials](https://console.cloud.google.com/apis/credentials)

5. Select or Create Service Account Key and download it as `cloud-credentials.json` file

6. Add dependencies to your module's build.gradle:
```kotlin
  repositories {
      maven("https://dl.bintray.com/aimybox/aimybox-android-sdk/")
  }
  
  dependencies {
      implementation("com.justai.aimybox:core:${version}")
      implementation("com.justai.aimybox:google-cloud-speechkit:${version}")
  }
```

7. Put the `cloud-credentials.json` file into assets, raw resources or file

8. Authorize using `GoogleCloudCredentials` and provide Google Cloud Speechkit components into Aimybox configuration object:
```kotlin
    const val AIMYBOX_API_KEY = "your Aimybox API Key"
    
    fun createAimybox(context: Context, unitId: String): Aimybox {
    
        GoogleCloudCredentials.loadFromAsset(context, "cloud-credentials.json")
    
        val speechToText = GoogleCloudSpeechToText(Locale.ENGLISH)
        //val textToSpeech = GoogleCloudTextToSpeech()
        
        val dialogApi = AimyboxDialogApi(AIMYBOX_API_KEY, unitId)
        
        val config = Config.create(speechToText, textToSpeech, dialogApi)
    
        return Aimybox(config)
    }
```

## Documentation

There is a full Aimybox documentation available [here](https://help.aimybox.com)
