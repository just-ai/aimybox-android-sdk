# Dummy dialog API implementation for Aimybox Android SDK

You can use this module for testing purposes (for example to test some speech-to-text or text-to-speech modules).
It just echoes everything that speech-to-text module has recognised.

## How to start using

1. Add dependencies to your module's build.gradle:
```kotlin
    repositories {
        jcenter()
    }
    
    dependencies {
        implementation("com.justai.aimybox:core:${version}")
        implementation("com.justai.aimybox:dummy-api:${version}")
    }
```

2. Provide Dummy component into Aimybox configuration object:
```kotlin
    fun createAimybox(context: Context): Aimybox {
        val textToSpeech = GooglePlatformTextToSpeech(context, Locale.ENGLISH) // Or any other TTS
        val speechToText = GooglePlatformSpeechToText(context, Locale.ENGLISH) // Or any other ASR

        val dialogApi = DummyDialogApi()

        return Aimybox(Config.create(speechToText, textToSpeech, dialogApi))
    }
```