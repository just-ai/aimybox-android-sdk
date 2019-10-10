import com.justai.gradle.project.projectConfig
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.get

fun MavenPublication.configurePomFile(project: Project) = pom {
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
                dependency.write("runtime")
            }
            project.configurations["api"].dependencies.forEach { dependency ->
                dependency.write("compile")
            }
        }
    }
}