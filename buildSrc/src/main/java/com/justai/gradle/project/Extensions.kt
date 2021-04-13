package com.justai.gradle.project

import org.gradle.api.Project
import org.gradle.kotlin.dsl.KotlinBuildScript
import org.gradle.kotlin.dsl.extra


private const val EXTRA_PROJECT_CONFIG = "_extra_project_config"
private const val EXTRA_ROOT_PROJECT_CONFIG = "_extra_root_project_config"

fun Project.configureRootProject(closure: RootProjectConfig.() -> Unit) {
    check(name == rootProject.name) {
        "Trying to call rootProjectConfig {...} in \"$name\" " +
                "Root project config can only be initialized in project's build.gradle.kts"
    }
    rootProject.extra[EXTRA_ROOT_PROJECT_CONFIG] = RootProjectConfig().apply(closure)
}

val Project.rootProjectConfig: RootProjectConfig
    get() {
        require(rootProject.extra.has(EXTRA_ROOT_PROJECT_CONFIG)) {
            "Root project config is not defined. Add rootProjectConfig { ... } to your project's build.gradle.kts"
        }
        return rootProject.extra[EXTRA_ROOT_PROJECT_CONFIG] as RootProjectConfig
    }

fun Project.configureProject(closure: ProjectConfig.() -> Unit) = run {
    val config = ProjectConfig(name).apply(closure)
    extra[EXTRA_PROJECT_CONFIG] = config
}

val Project.projectConfig
    get() = if (extra.has(EXTRA_PROJECT_CONFIG)) {
        extra[EXTRA_PROJECT_CONFIG] as ProjectConfig
    } else {
        throw IllegalArgumentException(
            "Project \"$name\" is not configured. Add projectConfig { ... } to your module's build.gradle.kts"
        )
    }