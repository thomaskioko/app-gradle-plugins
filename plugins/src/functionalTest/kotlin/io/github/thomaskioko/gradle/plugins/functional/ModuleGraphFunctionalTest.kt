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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModuleGraphFunctionalTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `graphUpdate writes the transitive module graph into the module README`() {
        val project = Fixtures.extract("module-graph", tempFolder.newFolder("project"))

        project.runner("graphUpdate").build()

        val readme = project.rootDir.resolve("feature/README.md").readText()
        val expected = """
            |  :base[base]:::jvm-library
            |  :core[core]:::jvm-library
            |  :feature[feature]:::jvm-library
            |
            |  :core -.-> :base
            |  :feature --> :core
            |
        """.trimMargin()
        assertTrue("Expected the graph of :feature, got:\n$readme", readme.contains(expected))
    }

    @Test
    fun `graphUpdate leaves testing modules out of the graph`() {
        val project = Fixtures.extract("module-graph", tempFolder.newFolder("project"))

        project.runner("graphUpdate").build()

        val readme = project.rootDir.resolve("feature/README.md").readText()
        assertFalse("Expected no :core:testing node, got:\n$readme", readme.contains(":core:testing"))
        assertFalse(project.rootDir.resolve("core/testing/README.md").exists())
    }
}
