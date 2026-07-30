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

import org.gradle.testkit.runner.GradleRunner
import java.io.File

internal class FixtureProject(val rootDir: File) {
    fun runner(vararg args: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(rootDir)
            .withPluginClasspath()
            .withArguments(
                buildList {
                    addAll(args.asList())
                    add("--stacktrace")
                },
            )
            .forwardOutput()
}

internal object Fixtures {
    private const val DIR_PROPERTY = "io.github.thomaskioko.gradle.plugins.fixtures.dir"

    private val rootFixturesDir: File by lazy {
        val path = System.getProperty(DIR_PROPERTY)
            ?: error(
                "System property '$DIR_PROPERTY' is not set. " +
                    "Configure it on the functionalTest Test task.",
            )
        File(path).also {
            require(it.isDirectory) { "Fixtures directory does not exist: ${it.absolutePath}" }
        }
    }

    fun extract(name: String, into: File): FixtureProject {
        val source = File(rootFixturesDir, name)
        require(source.isDirectory) {
            "Fixture not found: ${source.absolutePath}"
        }
        source.copyRecursively(into, overwrite = true)
        return FixtureProject(into)
    }
}
