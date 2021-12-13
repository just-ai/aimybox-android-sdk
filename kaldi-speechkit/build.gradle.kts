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

project.configureAndroid {
    defaultConfig {
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86_64", "x86"))
        }
    }
}

//repositories {
//    maven("https://alphacephei.com/maven/")
//}

dependencies {
    implementation(project(":core"))
    implementation(Library.Kotlin.stdLib)
    implementation(Library.Android.appCompat)
    implementation(Library.Kotlin.coroutines)

    implementation("com.alphacephei:vosk-android:0.3.23")
    implementation ("net.java.dev.jna:jna:5.8.0@aar")
    implementation("com.neovisionaries:nv-websocket-client:2.9")
}
