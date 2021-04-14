import com.justai.gradle.project.configureRootProject
import com.justai.gradle.project.projectConfig

val coreProject: Project
    get() = rootProject.project("core")

buildscript {
    repositories {
        google()
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

val versionProject = "0.16.3"
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
        jcenter()
        mavenCentral()
        mavenLocal()
        maven("https://kotlin.bintray.com/kotlinx")
        maven("https://dl.bintray.com/aimybox/aimybox-android-sdk/")
    }
}

tasks.register("localPublishAll") {
    group = "aimybox"

    val publicationTasks = subprojects.mapNotNull { project ->
        if (project.projectConfig.createMavenPublication) "${project.name}:${project.projectConfig.mavenLocalPublicationTask}"
        else null
    }.toTypedArray()

    dependsOn(*publicationTasks)
}

tasks.register<Delete>("clean") {
    group = "aimybox:util"
    delete(*(allprojects.map { it.buildDir }.toTypedArray()))
    delete(".gradle")
}