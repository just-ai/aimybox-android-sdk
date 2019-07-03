repositories {
    maven("https://houndify.com/maven/") { name = "Houndify" }
}

dependencies {
    implementation("com.justai.aimybox:core:${Versions.aimybox}")

    implementation(Libraries.Kotlin.stdLib)
    implementation(Libraries.Android.appCompat)
    batchImplementation(Libraries.Kotlin.coroutines)

    implementation("hound.android:hound-sdk:1.4.0")
}
