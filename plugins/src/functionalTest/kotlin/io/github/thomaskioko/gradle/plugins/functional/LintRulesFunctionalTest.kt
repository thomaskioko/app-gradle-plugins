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
package io.github.thomaskioko.gradle.plugins.functional

import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

private const val LINT_RULES_DIR_PROPERTY = "io.github.thomaskioko.gradle.plugins.lint.rules.dir"

class LintRulesFunctionalTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val lintRulesDir = checkNotNull(System.getProperty(LINT_RULES_DIR_PROPERTY)) {
        "System property '$LINT_RULES_DIR_PROPERTY' is not set. Configure it on the functionalTest Test task."
    }

    @Test
    fun `enabling lint rules makes spotless report violations of the suite's rules`() {
        val project = Fixtures.extract("jvm-library", tempFolder.newFolder("project"))
        File(project.rootDir, "gradle.properties").writeText("app.enableLintRules=true\n")
        File(project.rootDir, "settings.gradle.kts").appendText(
            """

            dependencyResolutionManagement {
                repositories {
                    exclusiveContent {
                        forRepository { flatDir { dirs("$lintRulesDir") } }
                        filter { includeModule("io.github.thomaskioko.gradle.plugins", "lint-rules") }
                    }
                }
            }
            """.trimIndent(),
        )
        File(project.rootDir, "src/main/kotlin/sample/Sample.kt").apply { parentFile.mkdirs() }.writeText(
            """
            package sample

            import com.arkivanov.decompose.router.stack.pushNew

            class Sample {
                val navigate = ::pushNew
            }

            """.trimIndent(),
        )

        val result = project.runner("spotlessKotlinCheck").buildAndFail()

        assertTrue(result.output.contains("tvmaniac:no-mutating-router-import"))
    }
}
