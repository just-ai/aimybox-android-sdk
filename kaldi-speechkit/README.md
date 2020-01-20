# Kaldi Speechkit for Aimybox Android SDK

Offline speech-to-text developed by [Kaldi](https://github.com/kaldi-asr/kaldi) and [Vosk](https://github.com/alphacep/vosk) projects.

_This engine recognises speech without an internet connection._

## Example

Here is a [working example](https://github.com/just-ai/aimybox-android-assistant/tree/kaldi/app) of Android voice assistant powered by this module.

## How to start using

1. Download model for your language [from here](https://github.com/alphacep/kaldi-android-demo/releases)
2. Unzip a downloaded package to assets folder of your Android project
3. Add dependencies to your module's build.gradle:
```kotlin
    repositories {
        maven("https://dl.bintray.com/aimybox/aimybox-android-sdk/")
    }
    
    dependencies {
        implementation("com.justai.aimybox:core:${version}")
        implementation("com.justai.aimybox:dummy-api:${version}") // or any other Dialog API
        implementation("com.justai.aimybox:kaldi-speechkit:${version}")
    }
```
4. Provide Kaldi Speechkit components into Aimybox configuration object:
```kotlin
    fun createAimybox(context: Context): Aimybox {
        val textToSpeech = GooglePlatformTextToSpeech(context, Locale.getDefault()) // or any other TTS
        val speechToText = KaldiSpeechToText(KaldiAssets.fromApkAssets(this, "model"))

        val dialogApi = DummyDialogApi() // or any other Dialog API

        return Aimybox(Config.create(speechToText, textToSpeech, dialogApi))
    }
```