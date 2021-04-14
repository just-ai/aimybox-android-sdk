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

dependencies {
    implementation(project(":core"))

    implementation(Library.Kotlin.stdLib)
    implementation(Library.Kotlin.coroutines)

    implementation(Library.Android.appCompat)

    implementation("com.google.cloud:google-cloud-speech:1.22.0")
    implementation("com.google.cloud:google-cloud-texttospeech:0.117.0-beta")

    implementation("io.grpc:grpc-okhttp" version { grpc })
}
