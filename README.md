<h1 align="center">
    <br>
    <a href="https://aimybox.com"><img src="https://app.aimybox.com/assets/images/aimybox.png"
                                                                    height="200"></a>
    <br><br>
    Aimybox Android SDK
</h1>

<h4 align="center">Open source voice assistant SDK written in Kotlin</h4>

<p align="center">
    <a href="https://gitter.im/aimybox/community"><img src="https://badges.gitter.im/amitmerchant1990/electron-markdownify.svg"></a>
    <a href="https://twitter.com/intent/follow?screen_name=aimybox"><img alt="Twitter Follow" src="https://img.shields.io/twitter/follow/aimybox.svg?label=Follow%20on%20Twitter&style=popout"></a>
    <a href="https://travis-ci.org/just-ai/aimybox-android-sdk/"><img alt="Travis CI Build" src="https://travis-ci.org/just-ai/aimybox-android-sdk.svg?branch=master"></a>
    <a href="https://bintray.com/aimybox/aimybox-android-sdk/"><img alt="Bintray artifact" src="https://api.bintray.com/packages/aimybox/aimybox-android-sdk/core/images/download.svg"></a>
</p>

The only solution if you need to embed your own intelligent voice assistant into your existing application or device.

# Key Features

* Provides ready to use [UI components](https://github.com/just-ai/aimybox-android-assistant) for fast building of your voice assistant app
* Modular and independent from speech-to-text and text-to-speech vendors
* Provides ready to use speech-to-text and text-to-speech implementations like [Android platform speechkit](https://github.com/just-ai/aimybox-android-sdk/tree/master/google-platform-speechkit), [Google Cloud speechkit](https://github.com/just-ai/aimybox-android-sdk/tree/master/google-cloud-speechkit), [Houndify](https://github.com/just-ai/aimybox-android-sdk/tree/master/houndify-speechkit) or [Snowboy wake word trigger](https://github.com/just-ai/aimybox-android-sdk/tree/master/snowboy-speechkit)
* Works with any NLU providers like [Aimylogic](https://help.aimybox.com/en/article/aimylogic-webhook-5quhb1/) or [Dialogflow](https://help.aimybox.com/en/article/dialogflow-agent-cqdvjn/)
* Fully customizable and extendable, you can connect any other speech-to-text, text-to-speech and NLU services
* Open source under Apache 2.0, written in pure Kotlin
* Embeddable into any application or device running Android
* Voice skills logic and complexity is not limited by any restrictions
* Can interact with any local device services and local networks

# How to start using

1. Create a new Android project with next dependencies in the _build.gradle_ file

```kotlin
    android {
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }
    
    repositories {
        maven("https://dl.bintray.com/aimybox/aimybox-android-sdk/")
    }
    
    dependencies {
        implementation("com.justai.aimybox:core:0.4.0")
    }
```

2. Add one or more dependencies of third party speech-to-text and text-to-speech libraries. For example

```kotlin
implementation("com.justai.aimybox:google-platform-speechkit:0.4.0")
```

3. Create a new project in [Aimybox console](https://app.aimybox.com), enable some voice skills and **copy your project's API key**.

4. Instantiate [Aimybox](https://github.com/just-ai/aimybox-android-sdk/blob/master/core/src/main/java/com/justai/aimybox/Aimybox.kt) in your [Application](https://github.com/just-ai/aimybox-android-assistant/blob/master/app/src/main/java/com/justai/aimybox/assistant/AimyboxApplication.kt) class like that

```kotlin
val unitId = UUID.randomUUID().toString()
val textToSpeech = GooglePlatformTextToSpeech(context)
val speechToText = GooglePlatformSpeechToText(context)
val dialogApi = AimyboxDialogApi("your Aimybox project API key", unitId)
val aimybox = Aimybox(Config.create(speechToText, textToSpeech, dialogApi))
```

Now you can start talking with your voice assistant using `startRecognition()` method of [Aimybox](https://github.com/just-ai/aimybox-android-sdk/blob/master/core/src/main/java/com/justai/aimybox/Aimybox.kt).

# More details

Please refer to the [demo voice assistant](https://github.com/aimybox/aimybox-android-assistant) to see how to use Aimybox library in your project. There are much more features described in [our Android SDK documentation](https://help.aimybox.com/en/article/android-sdk-overview-1ih4xn7/).

# Documentation

There is a full Aimybox documentation available [here](https://help.aimybox.com)
