/*
 * Copyright 2026 Thomas Kioko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thomaskioko.gradle.plugins.checks

import com.diffplug.gradle.spotless.SpotlessExtension
import io.github.thomaskioko.gradle.plugins.properties.scaffoldProperties
import io.github.thomaskioko.gradle.plugins.utils.libs
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Applies Spotless with the project's ktlint configuration.
 *
 * [io.github.thomaskioko.gradle.plugins.BasePlugin] applies it to every module. Apply it on the
 * root project as well so Spotless also checks the root build scripts. The plugin applies
 * `com.diffplug.spotless` and configures Kotlin (`src/**/*.kt`), Kotlin Gradle scripts (`*.kts`),
 * and XML targets.
 *
 * With `app.enableLintRules=true` in `gradle.properties`, ktlint also runs the suite's
 * navigation, preview, and test naming rules from the
 * `io.github.thomaskioko.gradle.plugins:lint-rules` artifact. The plugin reads its own version
 * from the jar manifest's `Implementation-Version` attribute and asks for the same version of the
 * rules, so bumping the plugin also bumps the rules. Further ktlint rule sets come from the
 * module's own [CUSTOM_RULE_SETS_KEY] extra. The DSL configuration is deferred until
 * `afterEvaluate` so the module's build script can register its rule sets before Spotless reads
 * them.
 *
 * Spotless is skipped entirely on benchmark modules, where running the formatter has no value
 * and adds noise to the build graph.
 */
public class SpotlessPlugin : Plugin<Project> {
    /**
     * Applies Spotless and configures its formats once the project is evaluated, so the build
     * script can still register its rule sets first.
     *
     * @throws org.gradle.api.GradleException if `app.enableLintRules` is `true` and the plugin
     *   jar carries no `Implementation-Version`.
     */
    override fun apply(project: Project): Unit = with(project) {
        pluginManager.apply("com.diffplug.spotless")

        // Skip Spotless entirely for modules that don't need it
        if (shouldSkipSpotlessForProject(project)) {
            tasks.configureEach {
                if (it.name.startsWith("spotless")) {
                    it.enabled = false
                }
            }
            return@with
        }

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
        val moduleRuleSets = when (val value = project.extensions.extraProperties.properties[CUSTOM_RULE_SETS_KEY]) {
            is List<*> -> value.filterIsInstance<String>()
            is String -> value.split(",").map(String::trim).filter(String::isNotEmpty)
            else -> emptyList()
        }
        if (!project.scaffoldProperties().enableLintRules.get()) return moduleRuleSets
        return listOf(lintRulesCoordinate()) + moduleRuleSets
    }

    private fun lintRulesCoordinate(): String {
        val version = SpotlessPlugin::class.java.`package`?.implementationVersion?.takeIf { it.isNotBlank() }
            ?: throw GradleException(
                "app.enableLintRules needs the plugin version, and the plugin jar has no " +
                    "'Implementation-Version' in its manifest.",
            )
        return "io.github.thomaskioko.gradle.plugins:lint-rules:$version"
    }

    /**
     * Keys this plugin reads from the project's extra properties.
     */
    public companion object {
        /**
         * Key under the module's own `extra` where a build script registers Maven coordinates of
         * custom ktlint rule sets, as a list or a comma-separated string. [SpotlessPlugin] reads
         * it during `afterEvaluate` and passes it to ktlint through Spotless's
         * `customRuleSets(...)` step. The suite's own rules need no entry here; they come from
         * `app.enableLintRules`.
         */
        public const val CUSTOM_RULE_SETS_KEY: String = "app.spotless.customRuleSets"
    }
}
