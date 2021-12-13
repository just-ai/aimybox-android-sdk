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
    implementation(Library.Android.appCompat)
    implementation(Library.Kotlin.coroutines)

    implementation("com.google.cloud:google-cloud-dialogflow:2.6.2")
    //implementation("io.grpc:grpc-okhttp:1.36.0")
    implementation("io.grpc:grpc-okhttp" version { grpc })
}
