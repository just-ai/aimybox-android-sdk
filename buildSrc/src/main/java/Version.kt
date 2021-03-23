object Version {
    // Kotlin
    const val kotlin = "1.4.21"
    const val dokka = "1.4.20"
    const val coroutines = "1.4.2"

    //Plugins
    const val androidPlugin = "3.5.1"
    const val dexCountPlugin = "0.8.6"

    const val bintrayPlugin = "1.8.4"
    const val bintrayBuildInfoPlugin = "4.7.5"

    const val protobufPlugin = "0.8.15"

    // Libraries
    const val appCompat = "1.2.0"

    const val jaicf = "0.8.2"

    const val retrofit = "2.9.0"
    const val okHttp = "4.9.0"
    const val grpc = "1.35.0"
    const val kotson = "2.5.0"

    // Test
    const val mockk = "1.10.6"
    const val androidxTest = "1.3.0"
}

infix fun String.version(versionProvider: Version.() -> String) =
    "$this:${versionProvider(Version)}"