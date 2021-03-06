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
        classpath(Plugin.protobuf)
    }
}

val versionProject = "0.16.5"
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
        maven("https://kotlin.bintray.com/kotlinx")
        maven("https://alphacephei.com/maven")
        flatDir {
            setDirs(listOf("pocketsphinx-android-lib"))
        }
    }
}

tasks.register<Delete>("clean") {
    group = "aimybox:util"
    delete(*(allprojects.map { it.buildDir }.toTypedArray()))
    delete(".gradle")
}