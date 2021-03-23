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

dependencies {
    implementation(project(":core"))

    implementation(Library.Kotlin.stdLib)
    implementation(Library.Android.appCompat)
    implementation(Library.Kotlin.coroutines)

    api("com.justai.jaicf:core" version { jaicf })
    api("com.justai.jaicf:aimybox" version { jaicf })
}
