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

class RootPluginRequiredFunctionalTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `applying a subproject plugin without root plugin fails with remediation message`() {
        val project = Fixtures.extract("missing-root-plugin", tempFolder.newFolder("project"))

        val result = project.runner("help").buildAndFail()

        assertTrue(
            "Expected GradleException remediation message; got:\n${result.output}",
            result.output.contains("io.github.thomaskioko.gradle.plugins.root must be applied to the root project"),
        )
    }
}
