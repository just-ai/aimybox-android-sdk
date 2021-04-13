import org.gradle.plugin.use.PluginDependenciesSpec

private fun PluginDependenciesSpec.internal(plugin: String) = id("com.justai.aimybox.plugins.internal.$plugin")
val PluginDependenciesSpec.`aimybox-publish` get() = internal("publish")
val PluginDependenciesSpec.`aimybox-github-release` get() = internal("github")