package io.github.thomaskioko.gradle.plugins.checks

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Registers the suite's `lint-rules` artifact with Spotless's ktlint configuration.
 *
 * Apply once to the root project to enable the suite's navigation, preview, and test naming
 * lint rules across every subproject. The plugin reads its own version from the JAR manifest's
 * `Implementation-Version` attribute, constructs the
 * `io.github.thomaskioko.gradle.plugins:lint-rules:<own-version>` Maven coordinate, and writes
 * it into the [SpotlessPlugin.CUSTOM_RULE_SETS_KEY] extra on both the root project and every
 * subproject. [SpotlessPlugin] reads the extra during `afterEvaluate` and forwards it to ktlint
 * through Spotless's `customRuleSets(...)` step.
 *
 * Version coupling is automatic. Bumping the convention plugin version in the consumer's
 * `libs.versions.toml` automatically pulls the matching `lint-rules` artifact.
 *
 * ```kotlin
 * // root build.gradle.kts
 * plugins {
 *   alias(libs.plugins.app.lint)
 * }
 * ```
 */
public class LintPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val pluginVersion = readPluginVersion()
            ?: throw GradleException(
                "Cannot determine $PLUGIN_GROUP plugin version. " +
                    "Ensure the plugin JAR has 'Implementation-Version' in its manifest.",
            )
        val coordinates = "$PLUGIN_GROUP:$LINT_RULES_ARTIFACT_ID:$pluginVersion"

        target.addLintRulesArtifact(coordinates)
        if (target == target.rootProject) {
            target.subprojects { subproject ->
                subproject.addLintRulesArtifact(coordinates)
            }
        }

        target.pluginManager.apply("io.github.thomaskioko.gradle.plugins.spotless")
    }

    private fun Project.addLintRulesArtifact(coordinates: String) {
        val extra = extensions.extraProperties
        @Suppress("UNCHECKED_CAST")
        val current: List<String> = if (extra.has(SpotlessPlugin.CUSTOM_RULE_SETS_KEY)) {
            (extra.get(SpotlessPlugin.CUSTOM_RULE_SETS_KEY) as? List<String>).orEmpty()
        } else {
            emptyList()
        }
        if (coordinates !in current) {
            extra.set(SpotlessPlugin.CUSTOM_RULE_SETS_KEY, current + coordinates)
        }
    }

    private fun readPluginVersion(): String? =
        LintPlugin::class.java.`package`?.implementationVersion?.takeIf { it.isNotBlank() }

    private companion object {
        const val PLUGIN_GROUP = "io.github.thomaskioko.gradle.plugins"
        const val LINT_RULES_ARTIFACT_ID = "lint-rules"
    }
}
