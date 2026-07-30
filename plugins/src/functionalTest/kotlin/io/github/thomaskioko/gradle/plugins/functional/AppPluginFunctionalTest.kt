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

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppPluginFunctionalTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `applying app plugin registers bumpVersion task on the app subproject`() {
        val project = Fixtures.extract("app-only", tempFolder.newFolder("project"))

        val result = project.runner(":app:help", "--task", ":app:bumpVersion").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":app:help")?.outcome)
    }

    @Test
    fun `applying app plugin registers release task on the app subproject`() {
        val project = Fixtures.extract("app-only", tempFolder.newFolder("project"))

        val result = project.runner(":app:help", "--task", ":app:release").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":app:help")?.outcome)
    }

    @Test
    fun `applying app plugin wires android variant test tasks into root ciTest`() {
        val project = Fixtures.extract("app-only", tempFolder.newFolder("project"))

        val result = project.runner("help", "--task", "ciTest").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome)
    }
}
