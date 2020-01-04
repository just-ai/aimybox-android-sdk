import com.android.build.gradle.BaseExtension
import com.justai.gradle.project.projectConfig
import com.justai.gradle.project.rootProjectConfig
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*

fun Project.registerPublicationTasks() {
    tasks.register<Jar>("sourcesJar") {
        group = "aimybox:util"
        archiveClassifier.set("sources")
        from(project.the<BaseExtension>().sourceSets["main"].java.srcDirs)
    }

    tasks.register("prepareArtifacts") {
        group = "aimybox:util"
        dependsOn("assembleRelease", "sourcesJar")
    }

}

fun Project.configureMavenPublication() {
    apply(plugin = "maven-publish")
    configure<PublishingExtension> {
        publications {
            create<MavenPublication>(projectConfig.publicationName) {
                groupId = rootProjectConfig.groupId
                artifactId = projectConfig.name
                version = rootProjectConfig.version

                artifact("$buildDir/outputs/aar/${project.name}-release.aar")
                artifact(tasks["sourcesJar"])
                configurePomFile(project)
            }
        }
    }

    tasks.named("publish${projectConfig.publicationName}PublicationToMavenLocal").configure {
        dependsOn("prepareArtifacts")
    }
}