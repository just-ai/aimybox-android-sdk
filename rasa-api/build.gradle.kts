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

    implementation("com.squareup.retrofit2:retrofit" version { retrofit })
    implementation("com.squareup.okhttp3:logging-interceptor" version { okHttp })
}
