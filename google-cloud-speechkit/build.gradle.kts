dependencies {
    implementation("com.justai.aimybox:core:${Versions.aimybox}")

    implementation(Libraries.Android.appCompat)
    implementation(Libraries.Kotlin.stdLib)
    batchImplementation(Libraries.Kotlin.coroutines)

    implementation("io.grpc:grpc-okhttp:1.10.0")
    implementation("com.google.cloud:google-cloud-speech:1.0.0")
    implementation("com.google.cloud:google-cloud-texttospeech:0.97.0-beta")
    implementation("net.sourceforge.argparse4j:argparse4j:0.8.1")
}
