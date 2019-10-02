# Dialogflow API connector for Aimybox Android SDK

Dialog API module to connect Aimybox powered assistant to the [Google Dialogflow](https://dialogflow.com) agent.

## How to start using

1. [Create](https://cloud.google.com/dialogflow/docs/tutorials/) a Dialogflow agent that recognises corresponding user intents
2. [Obtain a service account JSON](https://dialogflow.com/docs/reference/v2-auth-setup) of your Dialogflow agent
3. Add this JSON file to your Android project's _raw_ folder (with _"account.json"_ name for example)
4. Add dependencies to your module's build.gradle:
```kotlin
    repositories {
        maven("https://dl.bintray.com/aimybox/aimybox-android-sdk/")
    }
    
    dependencies {
        implementation("com.justai.aimybox:core:${version}")
        implementation("com.justai.aimybox:dialogflow-api:${version}")
    }
```
5. Provide Dialogflow component into Aimybox configuration object:
```kotlin    
    fun createAimybox(context: Context, unitId: String): Aimybox {
        val locale = Locale.getDefault()
    
        val textToSpeech = GooglePlatformTextToSpeech(context, locale) // Or any other TTS
        val speechToText = GooglePlatformSpeechToText(context, locale) // Or any other ASR
        val dialogApi = DialogflowDialogApi(context, R.raw.account, locale.language)
        
        val config = Config.create(speechToText, textToSpeech, dialogApi)
    
        return Aimybox(config)
    }
```

## Supported response types
Dialogflow can response with simple text as well as Google Assistant's [suggestion buttons](https://developers.google.com/actions/assistant/responses#suggestion_chips) and [basic cards](https://developers.google.com/actions/assistant/responses#basic_card).
You can use these types because Aimybox converts it into images, texts, buttons and links.

## Documentation

There is a full Aimybox documentation available [here](https://help.aimybox.com)