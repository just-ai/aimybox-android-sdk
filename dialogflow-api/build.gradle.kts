import com.justai.gradle.project.configureProject


plugins {
    id("com.android.library")
}

configureProject {
    isLibrary = true
    createMavenPublication = true
    publishToBintray = true
}

dependencies {
    implementation(project(":core"))

    implementation(Library.Kotlin.stdLib)
    implementation(Library.Android.appCompat)
    implementation(Library.Kotlin.coroutines)

    implementation("com.google.cloud:google-cloud-dialogflow:0.109.0-alpha")
    implementation("io.grpc:grpc-okhttp:1.24.0")
}
