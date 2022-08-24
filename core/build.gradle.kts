import com.justai.gradle.project.configureAndroid
import com.justai.gradle.project.configureProject

plugins {
    id("com.android.library")
    `aimybox-publish`
}

project.configureProject {
    createMavenPublication = true
    isLibrary = true
}

project.configureAndroid {
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(Library.Android.appCompat)
    implementation(Library.Kotlin.stdLib)
    implementation(Library.Kotlin.coroutines)

    implementation("com.squareup.retrofit2:retrofit" version { retrofit })
    implementation("com.squareup.okhttp3:logging-interceptor" version { okHttp })
    implementation("com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:1.6.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    testImplementation ("org.robolectric:robolectric:4.6")
    testImplementation ("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.0")

    api("com.github.salomonbrys.kotson:kotson" version { kotson })

    api("io.grpc:grpc-okhttp" version { grpc })
    api("io.grpc:grpc-protobuf" version { grpc })
    api("io.grpc:grpc-stub" version { grpc })
    

    for (notation in (Library.Test.instrumentedTest)) {
        androidTestImplementation(notation)
    }

    for (notation in (Library.Test.unitTest)) {
        testImplementation(notation)
    }
}
