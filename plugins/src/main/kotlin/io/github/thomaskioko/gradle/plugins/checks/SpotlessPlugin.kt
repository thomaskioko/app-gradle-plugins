package io.github.thomaskioko.gradle.plugins.checks

import com.diffplug.gradle.spotless.SpotlessExtension
import io.github.thomaskioko.gradle.plugins.utils.libs
import org.gradle.api.Plugin
import org.gradle.api.Project

public class SpotlessPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = with(project) {
        pluginManager.apply("com.diffplug.spotless")

        // Skip Spotless entirely for modules that don't need it
        if (shouldSkipSpotlessForProject(project)) {
            afterEvaluate {
                tasks.matching { it.name.startsWith("spotless") }.configureEach {
                    it.enabled = false
                }
            }
            return@with
        }

        val ktlintVersion = libs.findVersion("ktlint").get().requiredVersion
        extensions.configure(SpotlessExtension::class.java) { extension ->
            with(extension) {
                kotlin {
                    it.ktlint(ktlintVersion).editorConfigOverride(
                        mapOf(
                            "android" to "true",
                        ),
                    )
                    it.target("**/*.kt")
                    it.targetExclude("${layout.buildDirectory}/**/*.kt")
                }

                kotlinGradle {
                    it.ktlint(ktlintVersion)
                    it.target("*.kts")
                }

                // Only configure XML formatting for projects that actually have XML files
                if (shouldConfigureXmlFormatting(project)) {
                    format("xml") {
                        it.target("src/**/*.xml")
                        it.targetExclude("**/build/", ".idea/")
                        it.trimTrailingWhitespace()
                        it.endWithNewline()
                    }
                }
            }
        }
    }

    private fun shouldSkipSpotlessForProject(project: Project): Boolean {
        return when {
            project.name == "benchmark" -> true
            project.path.contains(":benchmark") -> true
            else -> false
        }
    }

    private fun shouldConfigureXmlFormatting(project: Project): Boolean {
        return when {
            // Skip XML formatting for modules that typically don't have source XML files
            project.name == "benchmark" -> false
            project.path.contains(":benchmark") -> false
            // Only include XML formatting for Android modules that might have layouts/resources
            project.pluginManager.hasPlugin("com.android.application") -> true
            project.pluginManager.hasPlugin("com.android.library") -> true
            else -> false
        }
    }
}
