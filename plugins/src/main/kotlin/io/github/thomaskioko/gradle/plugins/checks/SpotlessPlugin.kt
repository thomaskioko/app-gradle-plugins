package io.github.thomaskioko.gradle.plugins.checks

import com.diffplug.gradle.spotless.SpotlessExtension
import io.github.thomaskioko.gradle.plugins.utils.libs
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Applies Spotless with the project's ktlint configuration.
 *
 * Applied automatically by [BasePlugin], so consumers do not apply this plugin directly. The
 * plugin applies `com.diffplug.spotless`, configures Kotlin (`src/**/*.kt`), Kotlin Gradle
 * scripts (`*.kts`), and XML targets, and reads any custom ktlint rule sets from the
 * [CUSTOM_RULE_SETS_KEY] extra (typically populated by [LintPlugin]). The DSL configuration is
 * deferred until `afterEvaluate` so plugins applied later in the same project can register their
 * rule sets before Spotless reads them.
 *
 * Spotless is skipped entirely on benchmark modules, where running the formatter has no value
 * and adds noise to the build graph.
 */
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

        // Defer the Spotless DSL configuration so plugins applied later in the same project
        // (notably LintPlugin via subprojects {}) have a chance to populate
        // CUSTOM_RULE_SETS_KEY before the value is read.
        afterEvaluate {
            val ktlintVersion = libs.findVersion("ktlint").get().requiredVersion
            val customRuleSets = readCustomKtlintRuleSets(project)

            extensions.configure(SpotlessExtension::class.java) { extension ->
                with(extension) {
                    kotlin {
                        val ktlintStep = it.ktlint(ktlintVersion)
                        if (customRuleSets.isNotEmpty()) {
                            ktlintStep.customRuleSets(customRuleSets)
                        }
                        ktlintStep.editorConfigOverride(
                            mapOf(
                                "android" to "true",
                            ),
                        )
                        it.target("src/**/*.kt")
                    }

                    kotlinGradle {
                        val ktlintStep = it.ktlint(ktlintVersion)
                        if (customRuleSets.isNotEmpty()) {
                            ktlintStep.customRuleSets(customRuleSets)
                        }
                        it.target("*.kts")
                    }

                    format("xml") {
                        it.target("src/**/*.xml")
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

    private fun readCustomKtlintRuleSets(project: Project): List<String> {
        return when (val value = project.findProperty(CUSTOM_RULE_SETS_KEY)) {
            is List<*> -> value.filterIsInstance<String>()
            is String -> value.split(",").map(String::trim).filter(String::isNotEmpty)
            null -> emptyList()
            else -> emptyList()
        }
    }

    public companion object {
        /**
         * Key under `extra` where consumers register Maven coordinates of custom ktlint rule
         * sets. The list of coordinates is read by [SpotlessPlugin] during `afterEvaluate` and
         * passed to ktlint through Spotless's `customRuleSets(...)` step. [LintPlugin]
         * populates this key with the suite's `lint-rules` artifact.
         */
        public const val CUSTOM_RULE_SETS_KEY: String = "app.spotless.customRuleSets"
    }
}
