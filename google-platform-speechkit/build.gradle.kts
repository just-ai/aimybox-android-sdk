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

    implementation(Library.Android.appCompat)
    implementation(Library.Kotlin.stdLib)
    implementation(Library.Kotlin.coroutines)
}
