# Yandex Speechkit for Aimybox Android SDK

Speech recognition and synthesis by [Yandex Cloud Speechkit](https://cloud.yandex.ru/services/speechkit)

## How to start using

1. Login or register at [Yandex Cloud Console](https://console.cloud.yandex.ru/)
2. Make sure that your [billing account](https://cloud.yandex.ru/docs/billing/concepts/billing-account) is active
3. Get a [folder ID](https://cloud.yandex.ru/docs/resource-manager/operations/folder/get-id)
4. Create an [API key](https://cloud.yandex.ru/docs/iam/operations/api-key/create)
5. Add dependencies to your module's build.gradle:
```kotlin
    repositories {
        maven("https://dl.bintray.com/aimybox/aimybox-android-sdk/")
    }
    
    dependencies {
        implementation("com.justai.aimybox:core:${version}")
        implementation("com.justai.aimybox:yandex-speechkit:${version}")
    }
```
6. Provide Yandex Speechkit components into Aimybox configuration object:
```kotlin
    const val API_KEY = "your API Key"
    const val FOLDER_ID = "your folder id"
    const val AIMYBOX_API_KEY = "your Aimybox API Key"
    
    fun createAimybox(context: Context, unitId: String): Aimybox {
        val language = Language.EN
    
        val speechToText = YandexSpeechToText(API_KEY, FOLDER_ID, language)
        val textToSpeech = YandexTextToSpeech(context, API_KEY, FOLDER_ID, language)
        val dialogApi = AimyboxDialogApi(AIMYBOX_API_KEY, unitId)
        
        val config = Config.create(speechToText, textToSpeech, dialogApi)
    
        return Aimybox(config)
    }
```

## Documentation

There is a full Aimybox documentation available [here](https://help.aimybox.com)
