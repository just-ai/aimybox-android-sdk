dependencies {
    implementation("com.justai.aimybox:core:${Versions.aimybox}")

    implementation(Libraries.Kotlin.stdLib)
    implementation(Libraries.Android.appCompat)
    implementation(Libraries.Kotlin.coroutines)

    implementation(fileTree("libs"))
}
