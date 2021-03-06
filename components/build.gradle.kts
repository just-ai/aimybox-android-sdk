import com.justai.gradle.project.configureAndroid
import com.justai.gradle.project.configureProject
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    `aimybox-publish`
}

project.configureProject {
    isLibrary = true
    createMavenPublication = true
}

project.configureAndroid {
    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}

repositories {
    mavenLocal()
    google()
    mavenCentral()
    maven("https://kotlin.bintray.com/kotlinx")
}

dependencies {
    implementation(Library.Kotlin.stdLib)
    implementation(Library.Kotlin.coroutines)
    implementation(Library.Android.appCompat)
    implementation(Library.Android.recyclerView)
    implementation(Library.Android.constraintLayout)
    implementation("androidx.core:core-ktx:1.5.0")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("com.google.android.material:material:1.3.0")
    implementation("com.github.bumptech.glide:glide:4.9.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.9.0")
    implementation(project(":core"))
}
