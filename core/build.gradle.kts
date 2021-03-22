import com.justai.gradle.project.configureAndroid
import com.justai.gradle.project.configureProject

plugins {
    id("com.android.library")
}

configureProject {
    createMavenPublication = true
    isLibrary = true
    publishToBintray = true
}

configureAndroid {}

dependencies {
    implementation(Library.Android.appCompat)
    implementation(Library.Kotlin.stdLib)
    implementation(Library.Kotlin.coroutines)

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.2")
    implementation("com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:1.4.3-native-mt")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    api("com.github.salomonbrys.kotson:kotson:2.5.0")

    for (notation in (Library.Test.instrumentedTest + Library.Test.unitTest)) {
        testImplementation(notation)
    }
}
