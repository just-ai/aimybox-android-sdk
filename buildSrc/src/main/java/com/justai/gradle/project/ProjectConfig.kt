package com.justai.gradle.project

data class ProjectConfig(
    var name: String,
    var isLibrary: Boolean = false,
    var createMavenPublication: Boolean = false
) {
    val publicationName = name.split("-").joinToString("", transform = String::capitalize)

    val mavenLocalPublicationTask = "publish${publicationName}PublicationToMavenLocal"
}
