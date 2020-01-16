# Pocketsphinx Speechkit for Aimybox Android SDK

Offline speech-to-text and voice trigger by [CMU Pocketsphinx](https://github.com/cmusphinx/pocketsphinx-android)

_This module performs speech recognition without internet connection._

## Example

Here is a [working example](https://github.com/just-ai/aimybox-android-assistant/tree/pocketsphinx/app) of Android voice assistant powered by this module.

## How to start using

1. Download acoustic and language model for your language [from here](https://sourceforge.net/projects/cmusphinx/files/Acoustic%20and%20Language%20Models/)
2. Unzip a downloaded package to assets folder of your Android project
3. Create [dictionary](https://github.com/just-ai/aimybox-android-assistant/blob/pocketsphinx/app/src/main/assets/model/en/dictionary.dict) and [JSGF grammar](https://github.com/just-ai/aimybox-android-assistant/blob/pocketsphinx/app/src/main/assets/model/en/grammar.gram) files and place them in assets folder
4. Add dependencies to your module's build.gradle:
```kotlin
    repositories {
        maven("https://dl.bintray.com/aimybox/aimybox-android-sdk/")
    }
    
    dependencies {
        implementation("com.justai.aimybox:core:${version}")
        implementation("com.justai.aimybox:dummy-api:${version}") // or any other Dialog API
        implementation("com.justai.aimybox:pocketsphinx-speechkit:${version}")
    }
```
5. Provide Pocketpshinx Speechkit components into Aimybox configuration object:
```kotlin
    fun createAimybox(context: Context, unitId: String): Aimybox {
        val assets = PocketsphinxAssets
            .fromApkAssets(
                context,
                acousticModelFileName = "model",
                dictionaryFileName = "dictionary.dict",
                grammarFileName = "grammar.gram"
            )

        val provider = PocketsphinxRecognizerProvider(assets)

        val textToSpeech = GooglePlatformTextToSpeech(context, Locale.getDefault()) // or any other TTS
        val speechToText = PocketsphinxSpeechToText(provider, assets.grammarFilePath!!)
        val voiceTrigger = PocketsphinxVoiceTrigger(provider, getString(R.string.keyphrase)) // if you need voice trigger
        val dialogApi = DummyDialogApi() // or any other Dialog API

        return Aimybox(Config.create(speechToText, textToSpeech, dialogApi) {
            this.voiceTrigger = voiceTrigger
        })
    }
```

## Voice trigger

You can use voice trigger independently from the speech-to-text engine.
This means that you can use some other speech-to-text (for example [Google Platform](https://github.com/just-ai/aimybox-android-sdk/tree/master/google-platform-speechkit))
with voice trigger powered by Pocketpshinx.