import com.justai.gradle.project.configureAndroid
import com.justai.gradle.project.configureProject

plugins {
    id("com.android.library")
    `aimybox-publish`
}

project.configureProject {
    isLibrary = true
    createMavenPublication = true
}

project.configureAndroid {}

repositories {
    maven("https://dl.bintray.com/alphacep/vosk")
}

dependencies {
    implementation(project(":core"))
    implementation(Library.Kotlin.stdLib)
    implementation(Library.Android.appCompat)
    implementation(Library.Kotlin.coroutines)

    implementation("com.alphacep:vosk-android:0.3.17")
    implementation("com.neovisionaries:nv-websocket-client:2.9")
}
