# Rasa.ai API connector for Aimybox Android SDK

Dialog API module to connect Aimybox powered assistant to the [Rasa](https://rasa.ai) model.

## How to start using

1. [Install Rasa](https://rasa.com/docs/rasa/user-guide/installation/)
2. Create a new Rasa project via `rasa init --no-prompt` command in terminal
3. Run `rasa run` to start Rasa server and copy it's URL
4. Add dependencies to your module's build.gradle:
```kotlin
    repositories {
        mavenCentral()
    }
    
    dependencies {
        implementation("com.just-ai.aimybox:core:${version}")
        implementation("com.just-ai.aimybox:rasa-api:${version}")
    }
```
5. Provide Rasa component into Aimybox configuration object:
```kotlin
    fun createAimybox(context: Context): Aimybox {
        val sender = UUID.randomUUID().toString()
        val webhookUrl = "<webhook URL>/webhooks/rest/webhook"

        val textToSpeech = GooglePlatformTextToSpeech(context, Locale.ENGLISH) // Or any other TTS
        val speechToText = GooglePlatformSpeechToText(context, Locale.ENGLISH) // Or any other ASR

        val dialogApi = RasaDialogApi(sender, webhookUrl)

        return Aimybox(Config.create(speechToText, textToSpeech, dialogApi))
    }
```

## Tutorial
Here is a great [tutorial](https://blog.rasa.com/how-to-build-a-mobile-voice-assistant-with-open-source-rasa-and-aimybox/) on how to use Aimybox with Rasa.

## Documentation

There is a full Aimybox documentation available [here](https://help.aimybox.com)
