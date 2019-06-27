
object Versions {
    object Sdk {
        const val min = 19
        const val target = 28
        const val compile = target
    }

    const val aimybox = "0.0.1"

    const val kotlin = "1.3.40"
    const val androidGradle = "3.4.1"

    const val appCompat = "1.0.2"
    const val coroutines = "1.3.0-M1"

    const val retrofit = "2.5.0"
    const val okHttp = "3.14.1"
    const val kotson = "2.5.0"

    const val dexCount = "0.8.6"

    const val protobuf = "0.8.8"

    const val mockk = "1.9"
    const val androidxTest = "1.1.1"
}

object Plugins {
    val androidGradle = "com.android.tools.build:gradle" version Versions.androidGradle
    val kotlin = kotlin("gradle-plugin")
    val protobuf = "com.google.protobuf:protobuf-gradle-plugin" version Versions.protobuf
    val dexcount = "com.getkeepsafe.dexcount:dexcount-gradle-plugin" version Versions.dexCount
}

object Libraries {
    object Kotlin {
        val stdLib = kotlin("stdlib")

        val coroutinesCore = kotlinx("coroutines-core", Versions.coroutines)
        val coroutinesAndroid = kotlinx("coroutines-android", Versions.coroutines)

        val coroutines = listOf(coroutinesCore, coroutinesAndroid)
    }

    object Android {
        val appCompat = "androidx.appcompat:appcompat" version Versions.appCompat
    }


    object Test {
        val kotlin = kotlin("test")
        val kotlinJUnit = kotlin("test-junit")

        val mockk = "io.mockk:mockk" version Versions.mockk

        val androidXRunner = "androidx.test:runner" version Versions.androidxTest
        val androidXRules = "androidx.test:rules" version Versions.androidxTest

        val unitTest = listOf(kotlin, kotlinJUnit, mockk)
        val instrumentedTest = listOf(kotlin, androidXRunner, androidXRules)

    }
}

internal fun kotlin(module: String, version: String = Versions.kotlin) =
    "org.jetbrains.kotlin:kotlin-$module:$version"

internal fun kotlinx(module: String, version: String = Versions.kotlin) =
    "org.jetbrains.kotlinx:kotlinx-$module:$version"

internal infix fun String.version(version: String) = plus(":$version")