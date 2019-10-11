import com.jfrog.bintray.gradle.BintrayExtension
import com.justai.gradle.project.configureRootProject
import com.justai.gradle.project.projectConfig
import com.justai.gradle.project.rootProjectConfig

val coreProject: Project
    get() = rootProject.project("core")

val publishCoreToMavenLocalTask: TaskProvider<Task>
    get() = rootProject.tasks.named("publishCoreToMavenLocal")

buildscript {
    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath(Plugin.androidGradle)
        classpath(Plugin.kotlinGradle)
        classpath(Plugin.dokka)
        classpath(Plugin.dexcount)
        classpath(Plugin.bintray)
        classpath(Plugin.buildInfoExtractor)
        classpath(Plugin.protobuf)
    }
}

configureRootProject {
    kotlinVersion = Version.kotlin
    version = "0.5.0"
    versionCode = 1
    compileSdk = 29
    minSdk = 21
    groupId = "com.justai.aimybox"
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenCentral()
        mavenLocal()
        maven("https://kotlin.bintray.com/kotlinx")
        maven("https://dl.bintray.com/aimybox/aimybox-android-sdk/")
    }
}

subprojects {
    afterEvaluate {
        if (projectConfig.isMavenPublication) {
            registerPublicationTasks()
            configureMavenPublication()

            if (project != coreProject) {
                tasks.named(projectConfig.mavenLocalPublicationTask).configure {
                    dependsOn(publishCoreToMavenLocalTask)
                    mustRunAfter(publishCoreToMavenLocalTask)
                }

                rootProject.tasks.register(projectConfig.customMavenLocalPublicationTask) {
                    group = "aimybox:submodules"
                    dependsOn(tasks.named("clean"), tasks.named(projectConfig.mavenLocalPublicationTask))
                    mustRunAfter("clean")
                }
            }

            if (projectConfig.publishToBintray) {
                configureBintrayPublication()

                rootProject.tasks.register(projectConfig.customBintrayPublicationTask) {
                    group = "aimybox:submodules"
                    dependsOn(tasks.named("clean"), tasks.named("bintrayUpload"))
                    mustRunAfter("clean")
                }
            }
        }
    }
}

fun Project.configureBintrayPublication() {
    apply(plugin = "com.jfrog.bintray")
    configure<BintrayExtension> {
        val bintrayUsername = properties["bintrayUser"] as? String
            ?: System.getProperty("BINTRAY_USER") ?: System.getenv("BINTRAY_USER")
        val bintrayKey = properties["bintrayKey"] as? String
            ?: System.getProperty("BINTRAY_KEY") ?: System.getenv("BINTRAY_KEY")
        user = bintrayUsername
        key = bintrayKey

        pkg(closureOf<BintrayExtension.PackageConfig> {
            repo = "aimybox-android-sdk"
            name = project.name
            userOrg = "aimybox"
            setLicenses("Apache-2.0")
            websiteUrl = "https://aimybox.com"
            publish = true
            vcsUrl = "https://github.com/aimybox/aimybox-android-sdk.git"
            version(closureOf<BintrayExtension.VersionConfig> { name = rootProjectConfig.version })
        })

        setPublications(projectConfig.publicationName)
    }
    tasks.named("bintrayUpload").configure {
        dependsOn("prepareArtifacts")
    }
}

tasks.register("publishCoreToMavenLocal") {
    group = "aimybox:submodules"
    dependsOn("clean", ":core:publishCorePublicationToMavenLocal")
    mustRunAfter("clean")
}

tasks.register("publishAllToMavenLocal") {
    group = "aimybox"
    dependsOn("clean")
    mustRunAfter("clean")

    val publicationTasks = subprojects.mapNotNull { project ->
        if (project.projectConfig.isMavenPublication) "${project.name}:${project.projectConfig.mavenLocalPublicationTask}"
        else null
    }.toTypedArray()

    dependsOn(*publicationTasks)
}

tasks.register("publishAllToBintray") {
    group = "aimybox"
    dependsOn("clean")
    mustRunAfter("clean")

    val publicationTasks = subprojects.mapNotNull { project ->
        if (project.projectConfig.publishToBintray) "${project.name}:bintrayUpload"
        else null
    }.toTypedArray()
    dependsOn(*publicationTasks)
}

tasks.register<Delete>("clean") {
    group = "aimybox:util"
    delete(*(allprojects.map { it.buildDir }.toTypedArray()))
}