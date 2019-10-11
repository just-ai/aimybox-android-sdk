package com.justai.gradle.project

data class ProjectConfig(
    var name: String,
    var isLibrary: Boolean = false,
    var isMavenPublication: Boolean = false,
    var publishToBintray: Boolean = false
) {
    val publicationName = name.split("-").joinToString(" ", transform = String::capitalize)
    val gradlePublicationName = name.split("-").joinToString("", transform = String::capitalize)

    val mavenLocalPublicationTask = "publish${gradlePublicationName}PublicationToMavenLocal"

    val customMavenLocalPublicationTask = "localPublish${gradlePublicationName}"
    val customBintrayPublicationTask = "bintrayPublish${gradlePublicationName}"
}
