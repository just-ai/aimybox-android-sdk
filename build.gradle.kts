import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.RecordingCopyTask
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.dsl.DoubleDelegateWrapper
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask

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
        classpath(Plugins.bintray)
        classpath(Plugins.buildInfo)
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

    if (name in Submodules.list) apply("../publish.gradle")
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

fun Project.configureAndroid() {

    apply(plugin = "com.android.library")
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

//fun Project.setupPublishing() {
//    apply(plugin = "com.jfrog.bintray")
//
//    val publicationName = project.name
//        .split('-')
//        .joinToString("", transform = String::capitalize)
//
//    val bintrayUsername = properties["bintrayUser"] as String?
//        ?: System.getProperty("BINTRAY_USER") ?: System.getenv("BINTRAY_USER")
//    val bintrayKey = properties["bintrayKey"] as String?
//        ?: System.getProperty("BINTRAY_KEY") ?: System.getenv("BINTRAY_KEY")
//
//    configurePublication(publicationName)
//
//    configure<BintrayExtension> {
//        user = bintrayUsername
//        key = bintrayKey
//        setPublications(publicationName)
//
//        pkg(closureOf<BintrayExtension.PackageConfig> {
//            repo = "aimybox-android-sdk"
//            name = project.name
//            userOrg = "aimybox"
//            setLicenses("Apache-2.0")
//            vcsUrl = "https://github.com/aimybox/aimybox-android-sdk.git"
//
//            version(closureOf<BintrayExtension.VersionConfig> {
//                name = Versions.aimybox
//            })
//        })
//    }
//}
//
//fun Project.configurePublication(publicationName: String) {
//
//}

//fun Project.configurePublication(publicationName: String) {
//    apply(plugin = "maven-publish")
//
//    val sourcesJar = tasks.register<Jar>("sourcesJar") {
//        classifier = "sources"
//        from(project.the<BaseExtension>().sourceSets["main"].java.srcDirs)
//    }
//
//    configure<PublishingExtension> {
//        repositories {
//            maven {
//                name = "BuildLocal"
//                url = uri("$buildDir/repo")
//            }
//        }
//        val groupName = "com.justai.aimybox"
//        publications {
//            create<MavenPublication>(publicationName) {
//                groupId = groupName
//                artifactId = project.name
//                version = Versions.aimybox
//                val releaseAar = "$buildDir/outputs/aar/${project.name}-release.aar"
//                artifact(releaseAar)
//                artifact(sourcesJar.get())
//                pom {
//                    name.set("Aimybox ${project.name.replace('-', ' ').capitalize()}")
//                    description.set("Aimybox Android SDK")
//                    url.set("https://github.com/aimybox/aimybox-android-sdk")
//                    organization {
//                        name.set("Aimybox")
//                        url.set("https://aimybox.com/")
//                    }
//                    scm {
//                        val scmUrl = "scm:git:git@github.com/aimybox/aimybox-android-sdk.git"
//                        connection.set(scmUrl)
//                        developerConnection.set(scmUrl)
//                        url.set(this@pom.url)
//                        tag.set("HEAD")
//                    }
//                    developers {
//                        developer {
//                            id.set("morfeusys")
//                            name.set("Dmitriy Chechyotkin")
//                            email.set("morfeusys@gmail.com")
//                            organization.set("Aimybox")
//                            organizationUrl.set("https://aimybox.com")
//                            roles.set(listOf("Project-Administrator", "Developer"))
//                        }
//                        developer {
//                            id.set("lambdatamer")
//                            name.set("Alexander Sirota")
//                            email.set("lambdatamer@gmail.com")
//                            organization.set("Aimybox")
//                            organizationUrl.set("https://aimybox.com")
//                            roles.set(listOf("Developer"))
//                        }
//                        developer {
//                            id.set("nikkas29")
//                            name.set("Nikita Kasenkov")
//                            organization.set("Aimybox")
//                            organizationUrl.set("https://aimybox.com")
//                            roles.set(listOf("Developer"))
//                        }
//                    }
//                    licenses {
//                        license {
//                            name.set("The Apache License, Version 2.0")
//                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
//                        }
//                    }
//                    withXml {
//                        asNode().appendNode("dependencies").apply {
//                            fun Dependency.write(scope: String) = appendNode("dependency").apply {
//                                appendNode("groupId", group)
//                                appendNode("artifactId", name)
//                                appendNode("version", version)
//                                appendNode("scope", scope)
//                            }
//                            for (dependency in configurations["api"].dependencies) {
//                                dependency.write("compile")
//                            }
//                            for (dependency in configurations["implementation"].dependencies) {
//                                dependency.write("runtime")
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}

//fun Project.setupPublishing() {
//    val sourcesJar = tasks.register<Jar>("sourcesJar") {
//        classifier = "sources"
//        from(project.the<BaseExtension>().sourceSets["main"].java.srcDirs)
//    }
//
////    val javadoc = tasks.register<Javadoc>("javadoc") {
////        setSource(project.the<BaseExtension>().sourceSets["main"].java.srcDirs)
////        classpath += files(project.the<BaseExtension>().bootClasspath)
////
////        project.the<LibraryExtension>().libraryVariants.configureEach {
////            dependsOn(assemble)
////            classpath += files((javaCompiler as AbstractCompile).classpath)
////        }
////
////        // Ignore warnings about incomplete documentation
////        (options as StandardJavadocDocletOptions).apply {
////            addStringOption("Xdoclint:none", "true")
////            addStringOption("encoding", "UTF-8")
////        }
////
////    }
//
////    val javadocJar = tasks.register<Jar>("javadocJar") {
////        dependsOn(javadoc)
////        classifier = "javadoc"
////        from(javadoc.get().destinationDir)
////    }
//
//    artifacts.add("archives", sourcesJar)
////    artifacts.add("archives", javadocJar)
//
//    afterEvaluate {
//        tasks.register("prepareArtifacts") {
////            if (project.name == "core") dependsOn(javadocJar)
//            dependsOn(sourcesJar)
//            dependsOn("assembleRelease")
//        }
//
//        tasks.register("bintrayUploadAll") {
//            dependsOn(*Submodules.list.map {
//                ":$it:bintrayUpload"
//            }.toTypedArray())
//        }
//
//        tasks.named("bintrayUpload").configure { dependsOn("prepareArtifacts") }
//    }
//
//    val publicationName = project.name
//        .split('-')
//        .joinToString("", transform = String::capitalize)
//
//    apply(plugin = "maven-publish")
//    apply(plugin = "com.jfrog.artifactory")
//    apply(plugin = "com.jfrog.bintray")
//
//    configure<PublishingExtension> {
//        repositories {
//            maven {
//                name = "BuildLocal"
//                url = uri("$buildDir/repo")
//            }
//        }
//
//        val groupName = "com.justai.aimybox"
//
//        publications {
//            create<MavenPublication>(publicationName) {
//                groupId = groupName
//                artifactId = project.name
//                version = Versions.aimybox
//
//                val releaseAar = "$buildDir/outputs/aar/${project.name}-release.aar"
//                artifact(releaseAar)
//                artifact(sourcesJar.get())
////                artifact(javadocJar.get())
//
//                logger.info(
//                    """
//                    |Creating maven publication '$publicationName'
//                    |    Group: $groupName
//                    |    Artifact: $artifactId
//                    |    Version: $version
//                    |    Aar: $releaseAar
//                """.trimMargin()
//                )
//
//                pom {
//                    name.set("Aimybox ${project.name.replace('-', ' ').capitalize()}")
//                    description.set("Aimybox Android SDK")
//                    url.set("https://github.com/aimybox/aimybox-android-sdk")
//
//                    organization {
//                        name.set("Aimybox")
//                        url.set("https://aimybox.com/")
//                    }
//
//                    scm {
//                        val scmUrl = "scm:git:git@github.com/aimybox/aimybox-android-sdk.git"
//                        connection.set(scmUrl)
//                        developerConnection.set(scmUrl)
//                        url.set(this@pom.url)
//                        tag.set("HEAD")
//                    }
//
//                    developers {
//                        developer {
//                            id.set("morfeusys")
//                            name.set("Dmitriy Chechyotkin")
//                            email.set("morfeusys@gmail.com")
//                            organization.set("Aimybox")
//                            organizationUrl.set("https://aimybox.com")
//                            roles.set(listOf("Project-Administrator", "Developer"))
//                        }
//                        developer {
//                            id.set("lambdatamer")
//                            name.set("Alexander Sirota")
//                            email.set("lambdatamer@gmail.com")
//                            organization.set("Aimybox")
//                            organizationUrl.set("https://aimybox.com")
//                            roles.set(listOf("Developer"))
//                        }
//                        developer {
//                            id.set("nikkas29")
//                            name.set("Nikita Kasenkov")
//                            organization.set("Aimybox")
//                            organizationUrl.set("https://aimybox.com")
//                            roles.set(listOf("Developer"))
//                        }
//                    }
//
//                    licenses {
//                        license {
//                            name.set("The Apache License, Version 2.0")
//                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
//                        }
//                    }
//
//                    withXml {
//                        asNode().appendNode("dependencies").apply {
//                            fun Dependency.write(scope: String) = appendNode("dependency").apply {
//                                appendNode("groupId", group)
//                                appendNode("artifactId", name)
//                                appendNode("version", version)
//                                appendNode("scope", scope)
//                            }
//                            for (dependency in configurations["api"].dependencies) {
//                                dependency.write("compile")
//                            }
//                            for (dependency in configurations["implementation"].dependencies) {
//                                dependency.write("runtime")
//                            }
//                        }
//                    }
//
//                }
//            }
//        }
//    }
//
//    tasks.matching {
//        it.name.contains("publish") && it.name.contains("publication", true)
//    }.configureEach {
//        dependsOn("assembleRelease")
//    }
//
//    val bintrayUsername = properties["bintrayUser"] as String?
//        ?: System.getProperty("BINTRAY_USER") ?: System.getenv("BINTRAY_USER")
//    val bintrayKey = properties["bintrayKey"] as String?
//        ?: System.getProperty("BINTRAY_KEY") ?: System.getenv("BINTRAY_KEY")
//
//    configure<ArtifactoryPluginConvention> {
//        setContextUrl("https://oss.jfrog.org")
//        publish(closureOf<PublisherConfig> {
//            repository(closureOf<DoubleDelegateWrapper> {
//                invokeMethod("setRepoKey", "oss-snapshot-local")
//                invokeMethod("setUsername", bintrayUsername)
//                invokeMethod("setPassword", bintrayKey)
//            })
//        })
//    }
//
//    tasks.withType<ArtifactoryTask>().configureEach { publications(publicationName) }
//
//    configure<BintrayExtension> {
//        user = bintrayUsername
//        key = bintrayKey
//        setPublications(publicationName)
//
//        pkg(closureOf<BintrayExtension.PackageConfig> {
//            repo = "aimybox-android-sdk"
//            name = project.name
//            userOrg = "aimybox"
//            setLicenses("Apache-2.0")
//            vcsUrl = "https://github.com/aimybox/aimybox-android-sdk.git"
//
//            version(closureOf<BintrayExtension.VersionConfig> {
//                name = Versions.aimybox
//            })
//        })
//
//
//        logger.info("""
//            |Bintray configuration for '$publicationName'
//            |   User: $bintrayUsername
//            |   API key: $bintrayKey
//            |   Artifact name: ${pkg.name}
//            |   Repository: ${pkg.repo}
//            |   Organization: ${pkg.userOrg}
//            |   Version: ${pkg.version.name}
//        """.trimMargin())
//
//    }
//}