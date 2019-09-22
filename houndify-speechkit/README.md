# Houndify Speech Recognition for Aimybox Android SDK

Speech recognition by [Houndify](https://www.houndify.com/)

## How to start using

1. Login into your [Houndify dashboard](https://www.houndify.com/dashboard)
2. Select or create client
3. Copy Client ID and Client Key
4. Add dependencies to your module's build.gradle:
```kotlin
    repositories {
        maven("https://dl.bintray.com/aimybox/aimybox-android-sdk/")
    }
    
    dependencies {
        implementation("com.justai.aimybox:core:${version}")
        implementation("com.justai.aimybox:houndify-speechkit:${version}")
    }
```
5. Provide Houndify STT component into Aimybox configuration object:
```kotlin
    const val AIMYBOX_API_KEY = "your Aimybox API Key"
    const val CLIENT_ID = "your Houndify client ID"
    const val CLIENT_KEY = "your Houndify client key"
    
    fun createAimybox(context: Context, unitId: String): Aimybox {
        val language = Locale.getDefault()
    
        val textToSpeech = GooglePlatformTextToSpeech(context, language) // Or any other TTS
        val speechToText = HoundifySpeechToText(context, CLIENT_ID, CLIENT_KEY)
        val dialogApi = AimyboxDialogApi(AIMYBOX_API_KEY, unitId)
        
        val config = Config.create(speechToText, textToSpeech, dialogApi)
    
        return Aimybox(config)
    }
```

## Documentation

There is a full Aimybox documentation available [here](https://help.aimybox.com)
