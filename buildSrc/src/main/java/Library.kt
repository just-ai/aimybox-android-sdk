object Library {
    object Kotlin {
        val stdLib = kotlin("stdlib", "1.6.21")

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
        val kotlinCoroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test" version { Version.coroutines }

        val mockk = "io.mockk:mockk" version { mockk }
        val mockkAndroid = "io.mockk:mockk-android" version { mockk }

        val androidXRunner = "androidx.test:core" version { androidxTestCore }
        val androidXRules = "androidx.test:rules" version { androidxTestCore }
        val androidXExtJunit = "androidx.test.ext:junit" version { androidxTestExtJunit }

        val unitTest = listOf(
            kotlin,
            kotlinJUnit,
            kotlinCoroutinesTest,
            mockk
        )
        val instrumentedTest = listOf(
            kotlin,
            mockkAndroid,
            androidXRunner,
            androidXRules,
            androidXExtJunit
        )

    }
}