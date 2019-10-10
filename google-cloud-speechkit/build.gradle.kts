import com.justai.gradle.Libraries
import com.justai.gradle.project.projectConfig

projectConfig {
    isPublication = true
    isLibrary = true
}

dependencies {
    debugImplementation(project(":core"))
    releaseImplementation("com.justai.aimybox:core:${Versions.aimybox}")

    implementation(Libraries.Kotlin.stdLib)
    implementation(Libraries.Kotlin.coroutines)

    implementation("com.google.cloud:google-cloud-speech:1.13.0")
}
