dependencies {
    implementation("com.justai.aimybox:core:${Versions.aimybox}")

    implementation(Libraries.Android.appCompat)
    implementation(Libraries.Kotlin.stdLib)
    batchImplementation(Libraries.Kotlin.coroutines)

    implementation("io.grpc:grpc-okhttp:1.10.0")
    implementation("com.google.cloud:google-cloud-speech:0.41.0-alpha")
}
