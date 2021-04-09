package com.justai.plugins.publish

import com.android.build.gradle.BaseExtension
import com.justai.gradle.project.projectConfig
import com.justai.gradle.project.rootProjectConfig
import com.justai.plugins.PluginAdapter
import com.justai.plugins.apply
import com.justai.plugins.utils.applySafely
import com.justai.plugins.utils.loadLocalProperties
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import java.io.File
import java.net.URI

private const val SONATYPE_USER = "sonatype.user"
private const val SONATYPE_PASSWORD = "sonatype.password"
private const val SIGNING_KEY = "signing.keyId"
private const val SIGNING_PASS = "signing.password"
private const val SECRING_FILE = "signing.secretKeyRingFile"

private const val RELEASE_REPO = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
private const val SNAPSHOTS_REPO = "https://s01.oss.sonatype.org/content/repositories/snapshots/"

private const val MAVEN_CENTRAL = "MavenCentral"

class CentralPublishPlugin : Plugin<Project> by apply<CentralPublish>()

@Suppress("DuplicatedCode", "UnstableApiUsage")
class CentralPublish(project: Project) : PluginAdapter(project) {
    private val properties by lazy { loadLocalProperties() }
    private val secKey by lazy { properties.getProperty(SIGNING_KEY) }
    private val secPass by lazy { properties.getProperty(SIGNING_PASS) }
    private val secRing by lazy { properties.getProperty(SECRING_FILE) }
    private val sonatypeUser by lazy { properties.getProperty(SONATYPE_USER) }
    private val sonatypePassword by lazy { properties.getProperty(SONATYPE_PASSWORD) }

    override fun Project.apply() {
        applySafely<MavenPublishPlugin>()
        applySafely<SigningPlugin>()

        afterEvaluate {

            apply(plugin = "maven-publish")

            val sourcesJar = tasks.register<Jar>("sourcesJar") {
                archiveClassifier.set("sources")
                from(project.the<BaseExtension>().sourceSets["main"].java.srcDirs)
            }

            configurePublication(sourcesJar)
        }
    }

    private fun Project.configurePublication(sourcesJar: Any) {
        configure<PublishingExtension> {
            val isSnapshot = (rootProjectConfig.version).endsWith("SNAPSHOT")

            repositories {
                maven {
                    name = MAVEN_CENTRAL
                    url = when (isSnapshot) {
                        true -> URI(SNAPSHOTS_REPO)
                        false -> URI(RELEASE_REPO)
                    }
                    credentials {
                        username = sonatypeUser
                        password = sonatypePassword
                    }
                }
            }

            publications {
                create<MavenPublication>(project.name) {
                    configurePomFile(project)

                    artifact("$buildDir/outputs/aar/${project.name}-release.aar")
                    artifact(sourcesJar)
                }
            }

            if (isMavenCentralPublication) {
                configure<SigningExtension> {
                    useInMemoryPgpKeys(secKey, File(secRing).readText(), secPass)
                    sign(publications)
                }
            }
        }
    }
}

private val Project.isMavenCentralPublication: Boolean
    get() {
        val task = project.gradle.startParameter.taskNames.firstOrNull()
        val isPublish = task?.endsWith("publish") ?: false
        val isByCentralPublishTask = task?.endsWith("${MAVEN_CENTRAL}Repository") ?: false
        return isPublish || isByCentralPublishTask
    }

private fun Project.extraProperty(name: String) =
    project.extra.properties[name] as? String ?: error("No $name defined")

private fun MavenPublication.configurePomFile(project: Project) = pom {
    name.set("Aimybox ${project.projectConfig.publicationName}")
    description.set("A part of Aimybox Android SDK")
    url.set("https://github.com/aimybox/aimybox-android-sdk")
    organization {
        name.set("Aimybox")
        url.set("https://aimybox.com/")
    }
    scm {
        val scmUrl = "scm:git:git@github.com/aimybox/aimybox-android-sdk.git"
        connection.set(scmUrl)
        developerConnection.set(scmUrl)
        url.set(this@pom.url)
        tag.set("HEAD")
    }
    developers {
        developer {
            id.set("morfeusys")
            name.set("Dmitriy Chechyotkin")
            email.set("morfeusys@gmail.com")
            organization.set("Aimybox")
            organizationUrl.set("https://aimybox.com")
            roles.set(listOf("Project-Administrator", "Developer"))
        }
        developer {
            id.set("lambdatamer")
            name.set("Alexander Sirota")
            email.set("lambdatamer@gmail.com")
            organization.set("Aimybox")
            organizationUrl.set("https://aimybox.com")
            roles.set(listOf("Developer"))
        }
        developer {
            id.set("bgubanov")
            name.set("Boris Gubanov")
            email.set("b.gubanov@list.ru")
            organization.set("Aimybox")
            organizationUrl.set("https://aimybox.com")
            roles.set(listOf("Developer"))
        }
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
            project.configurations["implementation"].dependencies.forEach { dependency ->
                if (dependency is DefaultProjectDependency) {
                    DefaultExternalModuleDependency(
                        project.rootProjectConfig.groupId,
                        dependency.name.replace(":", ""),
                        project.rootProjectConfig.version,
                        "default"
                    ).write("runtime")
                } else {
                    dependency.write("runtime")
                }
            }
            project.configurations["api"].dependencies.forEach { dependency ->
                dependency.write("compile")
            }
        }
    }
}
