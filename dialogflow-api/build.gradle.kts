dependencies {
    implementation("com.justai.aimybox:core:${Versions.aimybox}")

    implementation(Libraries.Kotlin.stdLib)
    implementation(Libraries.Android.appCompat)
    implementation(Libraries.Kotlin.coroutines)

    implementation("com.google.cloud:google-cloud-dialogflow:0.109.0-alpha")
    implementation("io.grpc:grpc-okhttp:1.22.1")
}
