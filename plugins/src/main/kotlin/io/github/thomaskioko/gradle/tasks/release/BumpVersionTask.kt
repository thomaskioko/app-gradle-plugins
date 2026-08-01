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
package io.github.thomaskioko.gradle.tasks.release

import io.github.thomaskioko.gradle.plugins.utils.Versioning
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Modifies version.txt in place")
internal abstract class BumpVersionTask : DefaultTask() {

    init {
        description = "Bumps VERSION_NUMBER (major/minor/patch/beta) and updates BUILD_NUMBER in version.txt"
        group = "versioning"
    }

    @get:Internal
    internal abstract val versionFile: RegularFileProperty

    @get:Input
    internal abstract val bumpType: Property<String>

    @TaskAction
    internal fun bump() {
        val file = versionFile.get().asFile
        require(file.exists()) { "version.txt not found at ${file.path}" }

        val content = file.readText()
        val currentVersion = Versioning.parseVersion(content, file.path)
        val currentBuild = Versioning.parseBuildNumber(content, file.path)
        val type = bumpType.get()

        if (type == "beta") {
            val newBuild = Versioning.nextBeta(currentVersion, currentBuild)
            Versioning.writeVersionFile(file, content, currentVersion, newBuild)

            val betaIteration = newBuild - Versioning.compute(currentVersion)
            logger.lifecycle("$currentVersion beta $betaIteration (BUILD_NUMBER = $currentBuild -> $newBuild)")
        } else {
            val newVersion = Versioning.bump(currentVersion, type)
            val newBuild = Versioning.compute(newVersion)
            Versioning.writeVersionFile(file, content, newVersion, newBuild)

            logger.lifecycle("$currentVersion -> $newVersion (BUILD_NUMBER = $newBuild)")
        }
    }
}
