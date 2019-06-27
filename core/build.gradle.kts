plugins {
    kotlin("kapt")
}

dependencies {
    implementation(Libraries.Android.appCompat)
    implementation(Libraries.Kotlin.stdLib)
    batchImplementation(Libraries.Kotlin.coroutines)

    implementation("com.squareup.retrofit2:retrofit:${Versions.retrofit}")
    implementation("com.squareup.okhttp3:logging-interceptor:${Versions.okHttp}")
    implementation("com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2")
    implementation("com.squareup.retrofit2:converter-gson:${Versions.kotson}")
    api("com.github.salomonbrys.kotson:kotson:${Versions.kotson}")

    batchTestImplementation(Libraries.Test.unitTest)
    batchAndroidTestImplementation(Libraries.Test.instrumentedTest)
}
