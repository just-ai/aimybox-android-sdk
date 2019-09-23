# Google Platform Speechkit for Aimybox Android SDK

Speech recognition and synthesis by Google Services available on the most Android devices out of the box.

## How to start using

1. Make sure your device has Google Play Services installed
2. Add dependencies to your module's build.gradle:
```kotlin
    repositories {
        maven("https://dl.bintray.com/aimybox/aimybox-android-sdk/")
    }
    
    dependencies {
        implementation("com.justai.aimybox:core:${version}")
        implementation("com.justai.aimybox:google-platform-speechkit:${version}")
    }
```
3. Provide Google Platform Speechkit components into Aimybox configuration object:
```kotlin
    const val AIMYBOX_API_KEY = "your Aimybox API Key"
    
    fun createAimybox(context: Context, unitId: String): Aimybox {
        val language = Locale.getDefault()
    
        val textToSpeech = GooglePlatformTextToSpeech(context, language)
        val speechToText = GooglePlatformSpeechToText(context, language)
        val dialogApi = AimyboxDialogApi(AIMYBOX_API_KEY, unitId)
        
        val config = Config.create(speechToText, textToSpeech, dialogApi)
    
        return Aimybox(config)
    }
```

#### Offline recognition
If your language is supported by Google, you can use offline recognition:
```kotlin
    val speechToText = GooglePlatformSpeechToText(context, language, preferOffline = true)
```

## Documentation

There is a full Aimybox documentation available [here](https://help.aimybox.com)
