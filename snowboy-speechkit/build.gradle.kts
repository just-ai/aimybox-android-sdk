import com.android.tools.build.bundletool.model.utils.Versions
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
    implementation(Libraries.Android.appCompat)
    implementation(Libraries.Kotlin.coroutines)

    implementation("com.getkeepsafe.relinker:relinker:1.3.1")
}
