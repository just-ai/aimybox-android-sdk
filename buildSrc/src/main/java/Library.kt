object Library {
    object Kotlin {
        val stdLib = kotlin("stdlib")

        val coroutines =
            kotlinx("coroutines-android", Version.coroutines)
    }

    object Android {
        val appCompat = "androidx.appcompat:appcompat" version { appCompat }
        val recyclerView = "androidx.recyclerview:recyclerview" version { recyclerView }
        val constraintLayout = "androidx.constraintlayout:constraintlayout" version { constraintLayout }
    }

    object Test {
        val kotlin = kotlin("test")
        val kotlinJUnit = kotlin("test-junit")

        val mockk = "io.mockk:mockk-android" version { mockk }

        val androidXRunner = "androidx.test:runner" version { androidxTest }
        val androidXRules = "androidx.test:rules" version { androidxTest }

        val unitTest = listOf(
            kotlin,
            kotlinJUnit,
            mockk
        )
        val instrumentedTest = listOf(
            kotlin,
            androidXRunner,
            androidXRules
        )

    }
}