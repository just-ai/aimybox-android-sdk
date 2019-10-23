import com.justai.gradle.project.configureAndroid
import com.justai.gradle.project.configureProject

plugins {
    id("com.android.library")
}

configureProject {
    isLibrary = true
    createMavenPublication = true
    publishToBintray = false
}

configureAndroid {}

dependencies {
    implementation(project(":core"))

    implementation(Library.Kotlin.stdLib)
    implementation(Library.Kotlin.coroutines)

    implementation(Library.Android.appCompat)

    implementation("com.google.cloud:google-cloud-speech:1.13.0")

    implementation("io.grpc:grpc-okhttp" version { grpc })
}
