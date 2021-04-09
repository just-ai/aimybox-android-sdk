plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    maven("https://plugins.gradle.org/m2")
    jcenter()
}

dependencies {
    implementation("com.android.tools.build:gradle:4.1.0")
    implementation("com.github.breadmoirai:github-release:2.2.12")
    implementation(gradleApi())
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:0.10.0")
}
