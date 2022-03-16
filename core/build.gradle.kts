import com.justai.gradle.project.configureAndroid
import com.justai.gradle.project.configureProject

plugins {
    id("com.android.library")
    `aimybox-publish`
}

project.configureProject {
    createMavenPublication = true
    isLibrary = true
}

project.configureAndroid {
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(Library.Android.appCompat)
    implementation(Library.Kotlin.stdLib)
    implementation(Library.Kotlin.coroutines)

    implementation("com.squareup.retrofit2:retrofit" version { retrofit })
    implementation("com.squareup.okhttp3:logging-interceptor" version { okHttp })
    implementation("com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:1.4.3-native-mt")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    testImplementation ("org.robolectric:robolectric:4.6")
    testImplementation ("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.0")

    api("com.github.salomonbrys.kotson:kotson" version { kotson })

    for (notation in (Library.Test.instrumentedTest + Library.Test.unitTest)) {
        androidTestImplementation(notation)
        testImplementation(notation)
    }
}
