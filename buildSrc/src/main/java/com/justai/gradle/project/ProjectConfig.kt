package com.justai.gradle.project

data class ProjectConfig(
    var name: String,
    var isLibrary: Boolean = false,
    var isMavenPublication: Boolean = false,
    var publishToBintray: Boolean = false
) {
    val publicationName = name.replace("-", " ").capitalize()
    val publicationTaskName = name.split("-").joinToString("", transform = String::capitalize)
}
