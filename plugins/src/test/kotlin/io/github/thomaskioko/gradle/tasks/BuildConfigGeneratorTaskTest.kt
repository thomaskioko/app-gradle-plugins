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
package io.github.thomaskioko.gradle.tasks

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildConfigGeneratorTaskTest {

    private fun createTask(): BuildConfigGeneratorTask {
        val project = ProjectBuilder.builder().build()
        return project.tasks.register("generateBuildConfig", BuildConfigGeneratorTask::class.java).get()
    }

    @Test
    fun `generate writes the declared fields into BuildConfig`() {
        val task = createTask()
        task.packageName.set("com.thomaskioko.tvmaniac.core.base")
        task.stringFields.set(mapOf("TMDB_API_KEY" to "abc123"))
        task.booleanFields.set(mapOf("IS_DEBUG" to true))
        task.intFields.set(mapOf("TIMEOUT_SECONDS" to 30))

        task.generate()

        val generated = task.outputDirectory.get().asFile
            .resolve("com/thomaskioko/tvmaniac/core/base/BuildConfig.kt")
            .readText()

        assertTrue(generated, generated.contains("package com.thomaskioko.tvmaniac.core.base"))
        assertTrue(generated, generated.contains("""TMDB_API_KEY: String = "abc123""""))
        assertTrue(generated, generated.contains("IS_DEBUG: Boolean = true"))
        assertTrue(generated, generated.contains("TIMEOUT_SECONDS: Int = 30"))
    }
}
