dependencies {
    debugImplementation(project(":core"))
    releaseImplementation("com.justai.aimybox:core:${Versions.aimybox}")

    implementation(Libraries.Kotlin.stdLib)
    implementation(Libraries.Android.appCompat)
    implementation(Libraries.Kotlin.coroutines)

    implementation("com.getkeepsafe.relinker:relinker:1.3.1")
}
