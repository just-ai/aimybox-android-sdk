object Version {
    // Kotlin
    const val kotlin = "1.3.50"
    const val dokka = "0.10.0"
    const val coroutines = "1.3.1"

    //Plugins
    const val androidPlugin = "3.5.1"
    const val dexCountPlugin = "0.8.6"

    const val bintrayPlugin = "1.8.4"
    const val bintrayBuildInfoPlugin = "4.7.5"

    const val protobufPlugin = "0.8.10"

    // Libraries
    const val appCompat = "1.1.0"

    const val retrofit = "2.5.0"
    const val okHttp = "3.14.1"
    const val grpc = "1.24.0"
    const val kotson = "2.5.0"

    // Test
    const val mockk = "1.9"
    const val androidxTest = "1.1.1"
}

infix fun String.version(versionProvider: Version.() -> String) =
    "$this:${versionProvider(Version)}"