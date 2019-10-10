import com.justai.gradle.Libraries
import com.justai.gradle.project.projectConfig

projectConfig {
    isPublication = true
    isLibrary = true
}

repositories {
    maven("https://houndify.com/maven/") { name = "Houndify" }
}

dependencies {
    debugImplementation(project(":core"))
    releaseImplementation("com.justai.aimybox:core:${Versions.aimybox}")

    implementation(Libraries.Kotlin.stdLib)
    implementation(Libraries.Android.appCompat)
    implementation(Libraries.Kotlin.coroutines)

    implementation("hound.android:hound-sdk:1.4.0")
}
