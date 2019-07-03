dependencies {
    implementation("com.justai.aimybox:core:${Versions.Aimybox.core}")

    implementation(Libraries.Kotlin.stdLib)
    implementation(Libraries.Android.appCompat)
    batchImplementation(Libraries.Kotlin.coroutines)

    implementation("com.getkeepsafe.relinker:relinker:1.3.1")
}
