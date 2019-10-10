import com.justai.gradle.Libraries

projectConfig {
    isPublication = true
    isLibrary = true
}

dependencies {
    debugImplementation(project(":core"))
    releaseImplementation("com.justai.aimybox:core:${Versions.aimybox}")

    implementation(Libraries.Android.appCompat)
    implementation(Libraries.Kotlin.stdLib)
    implementation(Libraries.Kotlin.coroutines)
}
