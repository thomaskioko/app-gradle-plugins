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
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

@UntrackedTask(because = "Modifies version.txt, CHANGELOG.md and creates git commit + tag")
internal abstract class ReleaseTask @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {

    init {
        description = "Bumps version, updates changelog, commits, and tags."
        group = "versioning"
    }

    @get:Internal
    internal abstract val versionFile: RegularFileProperty

    @get:Internal
    internal abstract val changelogFile: RegularFileProperty

    @get:Internal
    internal abstract val cliffConfigFile: RegularFileProperty

    @get:Internal
    internal abstract val projectDir: DirectoryProperty

    @get:Input
    internal abstract val bumpType: Property<String>

    @get:Input
    internal abstract val beta: Property<Boolean>

    @get:Input
    internal abstract val dryRun: Property<Boolean>

    @TaskAction
    internal fun release() {
        requireGitCliff()

        val validTypes = setOf("major", "minor", "patch", "beta")
        require(bumpType.get() in validTypes) {
            "Invalid bump type '${bumpType.get()}'. Must be one of: ${validTypes.joinToString()}"
        }

        val file = versionFile.get().asFile
        require(file.exists()) { "version.txt not found at ${file.path}" }

        val content = file.readText()
        val currentVersion = Versioning.parseVersion(content, file.path)
        val currentBuild = Versioning.parseBuildNumber(content, file.path)
        val type = bumpType.get()
        val isBetaBump = type == "beta"
        val branch = currentBranch()

        if (isBetaBump) {
            val newBuild = Versioning.nextBeta(currentVersion, currentBuild)

            if (dryRun.get()) {
                logger.lifecycle("$currentVersion beta (BUILD_NUMBER = $currentBuild -> $newBuild)")
                logger.lifecycle("Dry run complete. No files modified, no commits created.")
                return
            }

            Versioning.writeVersionFile(file, content, currentVersion, newBuild)
            git("add", file.absolutePath)
            git("commit", "-m", "chore: bump beta build number to $newBuild")
            git("push", "origin", branch)

            logger.lifecycle("$currentVersion beta (BUILD_NUMBER = $currentBuild -> $newBuild)")
            logger.lifecycle("Pushed to origin/$branch.")
            return
        }

        val isBeta = beta.get()
        val newVersion = Versioning.bump(currentVersion, type)
        val newBuild = Versioning.compute(newVersion)
        val tag = buildTag(newVersion, isBeta, branch)

        runChecks(file.toRelativeString(projectDir.get().asFile), tag, isBeta, branch)

        val recentTags = recentReleaseTags()
        if (recentTags.isNotEmpty()) {
            logger.lifecycle("Recent releases: ${recentTags.joinToString(", ")}")
        }

        if (dryRun.get()) {
            printDryRun(currentVersion, newVersion, tag)
            return
        }

        Versioning.writeVersionFile(file, content, newVersion, newBuild)

        val changelog = changelogFile.get().asFile
        generateChangelog(changelog, tag)

        git("add", file.absolutePath)
        git("add", changelog.absolutePath)
        git("commit", "-m", "release: $tag")
        git("tag", "-a", tag, "-m", "Release $tag")
        git("push", "origin", branch, "--tags")

        logger.lifecycle("$currentVersion -> $newVersion (BUILD_NUMBER = $newBuild)")
        logger.lifecycle("Pushed $tag to origin/$branch.")
    }

    private fun printDryRun(currentVersion: String, newVersion: String, tag: String) {
        logger.lifecycle("$currentVersion → $newVersion")
        logger.lifecycle("Tag: $tag")

        val preview = previewChangelog(tag)
        if (preview.isNotBlank()) {
            logger.lifecycle("Changelog preview:")
            logger.lifecycle(preview)
        }

        logger.lifecycle("Dry run complete. No files modified, no commits or tags created.")
    }

    private fun runChecks(
        versionFilePath: String,
        tag: String,
        isBeta: Boolean,
        branch: String,
    ) {
        if (!isBeta) {
            require(branch == "main") { "Must be on 'main' branch to release, currently on '$branch'." }
        }

        val dirty = gitOutput("status", "--porcelain")
            .lines()
            .filter { it.isNotBlank() }
            .filter { line -> parsePorcelainPaths(line).none { it == versionFilePath } }
        require(dirty.isEmpty()) {
            "Working tree has uncommitted changes:\n${dirty.joinToString("\n")}\nCommit or stash them before releasing."
        }

        git("fetch", "--tags")
        val fetchResult = execOperations.exec {
            it.commandLine("git", "fetch", "origin", branch)
            it.isIgnoreExitValue = true
        }
        if (fetchResult.exitValue != 0) {
            logger.lifecycle("Remote branch 'origin/$branch' not found, pushing branch to origin.")
            git("push", "-u", "origin", branch)
        }

        val behind = gitOutput("rev-list", "--count", "HEAD..origin/$branch")
        require(behind == "0") { "Local branch is $behind commit(s) behind origin/$branch. Pull before releasing." }

        val existing = gitOutput("tag", "--list", tag)
        require(existing.isBlank()) { "Tag '$tag' already exists. Choose a different version or delete the existing tag." }
    }

    private fun recentReleaseTags(): List<String> =
        gitOutput("tag", "--list", "v*", "--sort=-version:refname")
            .lines()
            .filter { it.isNotBlank() }
            .take(5)

    private fun requireGitCliff() {
        val output = ByteArrayOutputStream()
        val errOutput = ByteArrayOutputStream()
        try {
            val result = execOperations.exec {
                it.commandLine("git-cliff", "--version")
                it.standardOutput = output
                it.errorOutput = errOutput
                it.isIgnoreExitValue = true
            }
            require(result.exitValue == 0) {
                "git-cliff is required but returned exit code ${result.exitValue}. " +
                    "Install it: brew install git-cliff\n" +
                    "See: https://git-cliff.org/docs/installation"
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            if (e is IllegalArgumentException) throw e
            throw IllegalStateException(
                "git-cliff is required but not found on PATH. " +
                    "Install it: brew install git-cliff\n" +
                    "See: https://git-cliff.org/docs/installation",
                e,
            )
        }
    }

    private fun cliffConfigArgs(): List<String> {
        val configFile = cliffConfigFile.orNull?.asFile
        return if (configFile != null && configFile.exists()) {
            listOf("--config", configFile.absolutePath)
        } else {
            emptyList()
        }
    }

    private fun previewChangelog(tag: String): String {
        val output = ByteArrayOutputStream()
        val errOutput = ByteArrayOutputStream()
        val result = execOperations.exec {
            it.commandLine(
                buildList {
                    add("git-cliff")
                    addAll(cliffConfigArgs())
                    add("--unreleased")
                    add("--tag")
                    add(tag)
                    add("--strip")
                    add("header")
                },
            )
            it.standardOutput = output
            it.errorOutput = errOutput
            it.isIgnoreExitValue = true
        }
        if (result.exitValue != 0) {
            val stderr = errOutput.toString().trim()
            logger.warn("git-cliff preview failed (exit ${result.exitValue}): $stderr")
            return ""
        }
        return output.toString().trim()
    }

    private fun generateChangelog(file: File, tag: String) {
        val errOutput = ByteArrayOutputStream()
        val result = execOperations.exec {
            it.commandLine(
                buildList {
                    add("git-cliff")
                    addAll(cliffConfigArgs())
                    add("--tag")
                    add(tag)
                    add("-o")
                    add(file.absolutePath)
                },
            )
            it.errorOutput = errOutput
            it.isIgnoreExitValue = true
        }
        require(result.exitValue == 0) {
            "git-cliff failed (exit ${result.exitValue}): ${errOutput.toString().trim()}"
        }
    }

    private fun currentBranch(): String =
        gitOutput("rev-parse", "--abbrev-ref", "HEAD")

    private fun gitOutput(vararg args: String): String {
        val output = ByteArrayOutputStream()
        execOperations.exec {
            it.commandLine("git", *args)
            it.standardOutput = output
        }
        return output.toString().trim()
    }

    private fun git(vararg args: String) {
        execOperations.exec {
            it.commandLine("git", *args)
        }
    }

    internal companion object {
        internal fun buildTag(versionName: String, isBeta: Boolean, branch: String): String =
            if (isBeta) "v$versionName-beta.${sanitizeBranchForTag(branch)}" else "v$versionName"

        internal fun sanitizeBranchForTag(branch: String): String =
            branch.replace(Regex("[^a-zA-Z0-9._-]"), "-")
                .replace(Regex("-{2,}"), "-")
                .trim('-')

        internal fun parsePorcelainPaths(line: String): List<String> {
            if (line.length < 3) return emptyList()
            val path = line.substring(3) // skip "XY "
            return if (" -> " in path) path.split(" -> ") else listOf(path)
        }
    }
}
