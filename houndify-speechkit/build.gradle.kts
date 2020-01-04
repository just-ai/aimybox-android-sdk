import com.justai.gradle.project.configureAndroid
import com.justai.gradle.project.configureProject

plugins {
    id("com.android.library")
}

configureProject {
    isLibrary = true
    createMavenPublication = true
    publishToBintray = true
}

configureAndroid {}

repositories {
    maven("https://houndify.com/maven/") { name = "Houndify" }
}

dependencies {
    implementation(project(":core"))

    implementation(Library.Kotlin.stdLib)
    implementation(Library.Android.appCompat)
    implementation(Library.Kotlin.coroutines)

    implementation("hound.android:hound-sdk:1.4.0")
}
