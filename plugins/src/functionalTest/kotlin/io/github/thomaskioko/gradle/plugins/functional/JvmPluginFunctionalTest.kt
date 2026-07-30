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

class JvmPluginFunctionalTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `applying jvm plugin registers aggregate ciTest task`() {
        val project = Fixtures.extract("jvm-library", tempFolder.newFolder("project"))

        val result = project.runner("help", "--task", "ciTest").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome)
    }

    @Test
    fun `applying jvm plugin registers test task linked into ciTest`() {
        val project = Fixtures.extract("jvm-library", tempFolder.newFolder("project"))

        val result = project.runner("help", "--task", "test").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome)
    }

    @Test
    fun `applying jvm plugin configures spotlessCheck task from base plugin`() {
        val project = Fixtures.extract("jvm-library", tempFolder.newFolder("project"))

        val result = project.runner("help", "--task", "spotlessCheck").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome)
    }
}
