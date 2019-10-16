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

    implementation(Library.Android.appCompat)
    implementation(Library.Kotlin.stdLib)
    implementation(Library.Kotlin.coroutines)
}
