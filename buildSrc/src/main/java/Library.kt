object Library {
    object Kotlin {
        val stdLib = kotlin("stdlib")

        val coroutines =
            kotlinx("coroutines-android", Version.coroutines)
    }

    object Android {
        val appCompat = "androidx.appcompat:appcompat" version { appCompat }
    }

    object Test {
        val kotlin = kotlin("test")
        val kotlinJUnit = kotlin("test-junit")

        val mockk = "io.mockk:mockk" version { mockk }

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