import com.jfrog.bintray.gradle.BintrayExtension
import com.justai.gradle.project.configureRootProject
import com.justai.gradle.project.projectConfig
import com.justai.gradle.project.rootProjectConfig

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
            if (projectConfig.publishToBintray) configureBintrayPublication()
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