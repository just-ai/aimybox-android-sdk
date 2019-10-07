dependencies {
    debugImplementation(project(":core"))
    releaseImplementation("com.justai.aimybox:core:${Versions.aimybox}")

    implementation(Libraries.Kotlin.stdLib)
    implementation(Libraries.Kotlin.coroutines)

    implementation("com.google.cloud:google-cloud-speech:1.13.0")
}
