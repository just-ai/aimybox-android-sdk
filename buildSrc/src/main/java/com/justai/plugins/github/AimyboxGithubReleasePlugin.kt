package com.justai.plugins.github

import com.github.breadmoirai.githubreleaseplugin.GithubReleaseExtension
import com.github.breadmoirai.githubreleaseplugin.GithubReleasePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import com.justai.plugins.PluginAdapter
import com.justai.plugins.apply
import com.justai.plugins.utils.applySafely
import com.justai.plugins.utils.isRoot
import com.justai.plugins.utils.loadLocalProperties

class AimyboxGithubReleasePlugin : Plugin<Project> by apply<AimyboxGithubRelease>()

class AimyboxGithubRelease(project: Project) : PluginAdapter(project) {
    private val properties by lazy { loadLocalProperties() }

    private val githubToken by lazy { properties.getProperty("github.token") }
    private val githubRepo = "aimybox-android-sdk"
    private val githubOwner = "just-ai"
    private val githubTagName by lazy { project.version.toString() }
    private val githubReleaseName by lazy { project.version.toString() }

    override fun Project.apply() {
        if (!isRoot) {
            logger.warn("Github release plugin should only be applied to the root project")
            return
        }

        applySafely<GithubReleasePlugin>()
    }

    override fun Project.afterEvaluated() {
        configure<GithubReleaseExtension> {
            setToken(githubToken)
            setOwner(githubOwner)
            setRepo(githubRepo)
            setTagName(githubTagName)
            setReleaseName(githubReleaseName)
            setOverwrite(true)
        }
    }
}