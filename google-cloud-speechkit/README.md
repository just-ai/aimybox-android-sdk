# Google Cloud Speechkit for Aimybox Android SDK

Speech recognition and synthesis by Google [Cloud Speech-to-Text](https://cloud.google.com/speech-to-text/) and [Cloud Text-to-Speech](https://cloud.google.com/text-to-speech/)

## How to start using

1. Login into [Google Cloud Console](https://console.cloud.google.com)
2. Select or create a project
3. Enable Speech-To-Text and/or Text-To-Speech API
4. Go to [Credentials](https://console.cloud.google.com/apis/credentials)
5. Select or Create Service Account Key, provide it an Owner role and download it as JSON file
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
7. Put the credentials JSON file into assets folder with name like `credentials.json`
8. Provide Google Cloud Speechkit components into Aimybox configuration object:
```kotlin
    const val AIMYBOX_API_KEY = "your Aimybox API Key"
    
    fun createAimybox(context: Context, unitId: String): Aimybox {
    
        val credentials = GoogleCloudCredentials.loadFromAsset(context, "credentials.json")
    
        val speechToText = GoogleCloudSpeechToText(credentials, Locale.getDefault())
        val textToSpeech = GoogleCloudTextToSpeech(context, credentials, Locale.getDefault())
        
        val dialogApi = AimyboxDialogApi(AIMYBOX_API_KEY, unitId)
        
        val config = Config.create(speechToText, textToSpeech, dialogApi)
    
        return Aimybox(config)
    }
```

### Additional configuration

There are a lot of additional configuration parameters available in `GoogleCloudSpeechToText.Config` and `GoogleCloudTextToSpeech.Config` classes.
Please use it to configure voice, rate and others parameters.

## Documentation

There is a full Aimybox documentation available [here](https://help.aimybox.com)
