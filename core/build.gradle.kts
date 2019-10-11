import com.justai.gradle.project.configureProject

plugins {
    id("com.android.library")
}

configureProject {
    isMavenPublication = true
    isLibrary = true
    publishToBintray = true
}

dependencies {
    implementation(Library.Android.appCompat)
    implementation(Library.Kotlin.stdLib)
    implementation(Library.Kotlin.coroutines)

    implementation("com.squareup.retrofit2:retrofit:${Version.retrofit}")
    implementation("com.squareup.okhttp3:logging-interceptor:${Version.okHttp}")
    implementation("com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2")
    implementation("com.squareup.retrofit2:converter-gson:${Version.kotson}")
    api("com.github.salomonbrys.kotson:kotson:${Version.kotson}")
}
