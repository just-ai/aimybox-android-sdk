# Kaldi Speechkit for Aimybox Android SDK

Speech-to-text engine developed by [Kaldi](https://github.com/kaldi-asr/kaldi) and [Vosk](https://github.com/alphacep/vosk) projects.
This module provides both speech-to-text and voice trigger components.

## Example

Here is a [working example](https://github.com/just-ai/aimybox-android-assistant/tree/kaldi/app) of Android voice assistant powered by this module.

## How to start using

Kaldi engine can work in both _offline_ and _online_ modes.

### Offline mode

In offline mode Kaldi utilizes a local model that is restricted due its tiny size and can produce less accurate results.

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
4. Provide Kaldi Speechkit component into Aimybox configuration object:
```kotlin
fun createAimybox(context: Context): Aimybox {
    val assets = KaldiAssets.fromApkAssets(this, "model")
    val speechToText = KaldiSpeechToText(assets)
    val textToSpeech = GooglePlatformTextToSpeech(context, Locale.getDefault()) // or any other TTS

    val dialogApi = DummyDialogApi() // or any other Dialog API

    return Aimybox(Config.create(speechToText, textToSpeech, dialogApi))
}
```

### Online mode

In online mode Kaldi connects to the remote hosting with running Kaldi websocket server.
In this case the size of the model hasn't to be tiny, that is why it can produce more accurate results.

_You don't have to download and serve any model data in this case._

1. Run Kaldi server as described [here](https://github.com/alphacep/kaldi-websocket-python)
2. Add dependencies to your module's build.gradle as described above
3. Provide Kaldi Speechkit component into Aimybox configuration object:
```kotlin
fun createAimybox(context: Context): Aimybox {
    val textToSpeech = GooglePlatformTextToSpeech(context, Locale.getDefault()) // or any other TTS
    val speechToText = KaldiWebsocketSpeechToText("your Kaldi server URL here") // or use wss://api.alphacephei.com/asr/en/ for testing purposes

    val dialogApi = DummyDialogApi() // or any other Dialog API

    return Aimybox(Config.create(speechToText, textToSpeech, dialogApi))
}
```

### Voice trigger

To use this engine as a word trigger you have to download a model as described [here](#offline-mode) and then initialize a voice trigger:

```kotlin
fun createAimybox(context: Context): Aimybox {
    val assets = KaldiAssets.fromApkAssets(this, "model")
    val voiceTrigger = KaldiVoiceTrigger(assets, listOf("listen", "hey"))    

    val textToSpeech = GooglePlatformTextToSpeech(context, Locale.getDefault()) // or any other TTS
    val speechToText = GooglePlatformSpeechToText(context, Locale.getDefault()) // or any other STT

    val dialogApi = DummyDialogApi() // or any other Dialog API

    return Aimybox(Config.create(speechToText, textToSpeech, dialogApi) {
        this.voiceTrigger = voiceTrigger
    })
}
```

## Documentation

There is a full Aimybox documentation available [here](https://help.aimybox.com)