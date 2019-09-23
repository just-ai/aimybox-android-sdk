# Snowboy Voice Trigger for Aimybox Android SDK

Hot word detection by [Snowboy](https://snowboy.kitt.ai/) 

## How to start using

1. Login into your [Snowboy account](https://snowboy.kitt.ai/dashboard)
2. Download suitable model file
3. Add dependencies to your module's build.gradle:
```kotlin
    repositories {
        maven("https://dl.bintray.com/aimybox/aimybox-android-sdk/")
    }
    
    dependencies {
        implementation("com.justai.aimybox:core:${version}")
        implementation("com.justai.aimybox:snowboy-speechkit:${version}")
    }
```
4. Provide Google Platform Speechkit components into Aimybox configuration object:
```kotlin
    const val AIMYBOX_API_KEY = "your Aimybox API Key"
    
    fun createAimybox(context: Context, unitId: String): Aimybox {
        val language = Locale.getDefault()
    
        val textToSpeech = GooglePlatformTextToSpeech(context, language) // Or any other TTS
        val speechToText = GooglePlatformSpeechToText(context, language) // Or any other STT
        val dialogApi = AimyboxDialogApi(AIMYBOX_API_KEY, unitId)
        
        val config = Config.create(speechToText, textToSpeech, dialogApi) {
            voiceTrigger = createSnowboy(context)
        }
    
        return Aimybox(config)
    }
    
    fun createSnowboy(context: Context): SnowboyVoiceTrigger {
        // If you provide snowboy assets with your APK
        val assets = SnowboyAssets
            .fromApkAssets(context, "model_file_name", "resource_file_name")
        // OR if you want to download assets from the internet
        val assets = SnowboyAssets
            .fromExternalStoragePath("path/to/snowboy/assets", "model_file_name", "resource_file_name")
        
        return SnowboyVoiceTrigger(context, assets)
        
    }
```

## Documentation

There is a full Aimybox documentation available [here](https://help.aimybox.com)
