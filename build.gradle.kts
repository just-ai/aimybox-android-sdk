import com.android.build.gradle.BaseExtension

val publishList = listOf(
    "core",
//    "google-cloud-speechkit",
    "google-platform-speechkit",
//    "houndify-speechkit",
    "snowboy-speechkit",
    "yandex-speechkit"
)

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath(Plugins.androidGradle)
        classpath(Plugins.kotlin)
        classpath(Plugins.protobuf)
        classpath(Plugins.dexcount)
    }
}

allprojects {
    repositories {
        mavenLocal()
        google()
        jcenter()
        mavenCentral()
        maven("https://kotlin.bintray.com/kotlinx")
    }

    if ((group as String).isNotEmpty()) configureAndroid()

    if (name in publishList) setupPublishing()
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

fun Project.configureAndroid() {
    logger.warn("Project name ${project.name}")
    if (name == "app") {
        apply(plugin = "com.android.application")
    } else {
        apply(plugin = "com.android.library")
    }
    apply(plugin = "kotlin-android")
    apply(plugin = "kotlin-android-extensions")
    apply(plugin = "com.getkeepsafe.dexcount")

    configure<BaseExtension> {
        compileSdkVersion(Versions.Sdk.compile)

        defaultConfig {
            minSdkVersion(Versions.Sdk.min)
            targetSdkVersion(Versions.Sdk.target)

            versionName = Versions.aimybox
            versionCode = 1
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        buildTypes {
            getByName("debug") {
                isMinifyEnabled = false
            }
            getByName("release") {
                //TODO configure pro guard
            }
        }

        lintOptions {
            isCheckAllWarnings = true
            isWarningsAsErrors = false
            isAbortOnError = true
        }
    }
}

fun Project.setupPublishing() {
    val sourcesJar = tasks.register<Jar>("sourcesJar") {
        classifier = "sources"
        from(project.the<BaseExtension>().sourceSets["main"].java.srcDirs)
    }

    artifacts.add("archives", sourcesJar)

    apply(plugin = "maven-publish")

    configure<PublishingExtension> {
        repositories {
            maven {
                name = "BuildLocal"
                url = uri("$buildDir/repo")
            }
        }

        val publicationName = project.name
            .split('-')
            .joinToString("", transform = String::capitalize)

        val groupName = "com.justai.aimybox"

        publications {
            create<MavenPublication>(publicationName) {
                groupId = groupName
                artifactId = project.name
                version = Versions.aimybox

                val releaseAar = "$buildDir/outputs/aar/${project.name}-release.aar"
                artifact(releaseAar)

                logger.info(
                    """
                    |Creating maven publication '$publicationName'
                    |    Group: $groupName
                    |    Artifact: $artifactId
                    |    Version: $version
                    |    Aar: $releaseAar
                """.trimMargin()
                )

                pom {
                    name.set("Aimybox ${project.name.replace('-', ' ').capitalize()}")
                    description.set("Aimybox Android SDK")
                    url.set("")
                    organization {
                        name.set("Just AI")
                        url.set("justai.com")
                    }
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    withXml {
                        asNode().appendNode("dependencies").apply {
                            fun Dependency.write(scope: String) = appendNode("dependency").apply {
                                appendNode("groupId", group)
                                appendNode("artifactId", name)
                                appendNode("version", version)
                                appendNode("scope", scope)
                            }
                            for (dependency in configurations["api"].dependencies) {
                                dependency.write("compile")
                            }
                            for (dependency in configurations["implementation"].dependencies) {
                                dependency.write("runtime")
                            }
                        }
                    }

                }
            }
        }
    }

    tasks.matching {
        it.name.contains("publish") && it.name.contains("publication", true)
    }.configureEach {
        dependsOn("assembleRelease")
    }
}