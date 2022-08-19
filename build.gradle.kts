import com.justai.gradle.project.configureRootProject

val coreProject: Project
    get() = rootProject.project("core")

buildscript {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }

    dependencies {
        classpath(Plugin.androidGradle)
        classpath(Plugin.kotlinGradle)
        classpath(Plugin.dokka)
        classpath(Plugin.bintray)
        classpath(Plugin.buildInfoExtractor)
        classpath("com.android.tools.build:gradle:7.1.3")
    }
}

val versionProject = "0.17.6-alpha.2"
configureRootProject {
    kotlinVersion = Version.kotlin
    version = versionProject
    versionCode = 1
    compileSdk = 30
    minSdk = 21
    groupId = "com.just-ai.aimybox"
}

allprojects {
    group = "com.just-ai.aimybox"
    version = versionProject
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven("https://alphacephei.com/maven")
        maven("https://houndify.com/maven/") { name = "Houndify" }
        maven("https://kotlin.bintray.com/kotlinx")
        flatDir {
            setDirs(listOf("pocketsphinx-android-lib"))
        }
    }
}

tasks.register<Delete>("clean") {
    delete(*(allprojects.map { it.buildDir }.toTypedArray()))
}
