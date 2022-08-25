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

    implementation(project(":core"))

    implementation("androidx.core:core-ktx:1.7.0")

    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test.ext:junit:1.1.3")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.4.0")

    implementation(Library.Kotlin.stdLib)
    implementation(Library.Kotlin.coroutines)

    //Retrofit
    api("com.squareup.retrofit2:retrofit:2.9.0")
    api("com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2")

}
