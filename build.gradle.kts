import com.jfrog.bintray.gradle.BintrayExtension
import com.justai.gradle.project.configureRootProject
import com.justai.gradle.project.projectConfig
import com.justai.gradle.project.rootProjectConfig

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
        classpath(Plugin.dexcount)
        classpath(Plugin.bintray)
        classpath(Plugin.buildInfoExtractor)
        classpath(Plugin.protobuf)
    }
}

configureRootProject {
    kotlinVersion = Version.kotlin
    version = "0.8.0"
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
        if (projectConfig.createMavenPublication) {
            registerPublicationTasks()
            configureMavenPublication()

            rootProject.tasks.register(projectConfig.customMavenLocalPublicationTask) {
                group = "aimybox:submodules"
                dependsOn(tasks.named(projectConfig.mavenLocalPublicationTask))
            }

            if (projectConfig.publishToBintray) {
                configureBintrayPublication()

                rootProject.tasks.register(projectConfig.bintrayPublicationTask) {
                    group = "aimybox:submodules"
                    dependsOn(
                        tasks.named("bintrayUpload"),
                        tasks.named(projectConfig.generatePomFileTask)
                    )
                }
            }
        }
    }
}

fun Project.configureBintrayPublication() {
    apply(plugin = "com.jfrog.bintray")
    configure<BintrayExtension> {
        val properties = java.util.Properties()
        val propsFile = project.rootProject.file("local.properties")

        if (propsFile.canRead()) {
            properties.load(propsFile.inputStream())
        }

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

tasks.register("localPublishAll") {
    group = "aimybox"

    val publicationTasks = subprojects.mapNotNull { project ->
        if (project.projectConfig.createMavenPublication) "${project.name}:${project.projectConfig.mavenLocalPublicationTask}"
        else null
    }.toTypedArray()

    dependsOn(*publicationTasks)
}

tasks.register("bintrayPublishAll") {
    group = "aimybox"

    val publicationTasks = subprojects.mapNotNull { project ->
        if (project.projectConfig.publishToBintray) project.projectConfig.bintrayPublicationTask
        else null
    }.toTypedArray()

    dependsOn(*publicationTasks)
}

tasks.register<Delete>("clean") {
    group = "aimybox:util"
    delete(*(allprojects.map { it.buildDir }.toTypedArray()))
    delete(".gradle")
}