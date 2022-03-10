import com.justai.gradle.project.configureAndroid
import com.justai.gradle.project.configureProject

plugins {
    kotlin("android")
    id("com.android.library")
    `aimybox-publish`
    id("org.jetbrains.kotlin.kapt")
}

project.configureProject {
    isLibrary = true
    createMavenPublication = true
}

project.configureAndroid {
//    tasks.withType<KotlinCompile> {
//        kotlinOptions.jvmTarget = "1.8"
//    }
}

repositories {
    mavenLocal()
    google()
    mavenCentral()
   // maven("https://kotlin.bintray.com/kotlinx")
}

dependencies {
    implementation(Library.Kotlin.stdLib)
    implementation(Library.Kotlin.coroutines)
    implementation(Library.Android.appCompat)
    implementation(Library.Android.recyclerView)
    implementation(Library.Android.constraintLayout)
    implementation("androidx.core:core-ktx:1.6.0")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("com.google.android.material:material:1.4.0")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    kapt("com.github.bumptech.glide:compiler:4.9.0")
    implementation(project(":core"))
}
