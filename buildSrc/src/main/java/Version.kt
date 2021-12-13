object Version {
    // Kotlin
    const val kotlin = "1.5.31"
    const val dokka = "1.4.20"
    const val coroutines = "1.5.2"

    //Plugins
    const val androidPlugin = "7.0.3"
    const val dexCountPlugin = "0.8.6"

    const val bintrayPlugin = "1.8.5"
    const val bintrayBuildInfoPlugin = "4.21.0"

    //const val protobufPlugin = "0.8.15"
    const val protobufPlugin = "0.8.18"

    // Libraries
    const val appCompat = "1.3.1"
    const val recyclerView = "1.2.0"
    const val constraintLayout = "2.1.2"

    const val jaicf = "1.2.1"

    const val retrofit = "2.9.0"
    const val okHttp = "4.9.0"
    //const val grpc = "1.35.0"
    const val grpc = "1.42.1"
    const val kotson = "2.5.0"

    // Test
    const val mockk = "1.10.6"
    const val androidxTest = "1.3.0"
}

infix fun String.version(versionProvider: Version.() -> String) =
    "$this:${versionProvider(Version)}"