package io.github.thomaskioko.gradle.tasks.release

import com.github.ajalt.mordant.terminal.ConversionResult
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.YesNoPrompt
import com.github.ajalt.mordant.terminal.prompt
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
public abstract class ReleaseTask @Inject constructor(
  private val execOperations: ExecOperations,
) : DefaultTask() {

  init {
    description = "Bumps version, updates changelog, commits, and tags. Pass -Pi for interactive mode."
    group = "versioning"
  }

  @get:Internal
  public abstract val versionFile: RegularFileProperty

  @get:Internal
  public abstract val changelogFile: RegularFileProperty

  @get:Internal
  public abstract val cliffConfigFile: RegularFileProperty

  @get:Internal
  public abstract val projectDir: DirectoryProperty

  @get:Input
  public abstract val bumpType: Property<String>

  @get:Input
  public abstract val beta: Property<Boolean>

  @get:Input
  public abstract val interactive: Property<Boolean>

  @get:Input
  public abstract val dryRun: Property<Boolean>

  @TaskAction
  public fun release() {
    requireGitCliff()

    if (interactive.get()) {
      runInteractive()
    } else {
      val validTypes = setOf("major", "minor", "patch")
      require(bumpType.get() in validTypes) {
        "Invalid bump type '${bumpType.get()}'. Must be one of: ${validTypes.joinToString()}"
      }
      runSilent()
    }
  }

  private fun runSilent() {
    val file = versionFile.get().asFile
    require(file.exists()) { "version.txt not found at ${file.path}" }

    val content = file.readText()
    val currentVersion = parseVersion(content, file.path)
    val result = Versioning.bump(currentVersion, bumpType.get())
    val isBeta = beta.get()
    val branch = currentBranch()
    val tag = buildTag(result.versionName, isBeta, branch)

    runChecks(file.toRelativeString(projectDir.get().asFile), tag, isBeta, branch) { label, block ->
      block()
    }

    val recentTags = recentReleaseTags()
    if (recentTags.isNotEmpty()) {
      logger.lifecycle("Recent releases: ${recentTags.joinToString(", ")}")
    }

    if (dryRun.get()) {
      printDryRun(currentVersion, result, tag)
      return
    }

    writeVersionFile(file, content, result)

    val changelog = changelogFile.get().asFile
    generateChangelog(changelog, tag)

    git("add", file.absolutePath)
    git("add", changelog.absolutePath)
    git("commit", "-m", "release: $tag")
    git("tag", "-a", tag, "-m", "Release $tag")

    logger.lifecycle("$currentVersion -> ${result.versionName} (BUILD_NUMBER = ${result.buildNumber})")
    logger.lifecycle("Created commit and tag: $tag")
    logger.lifecycle("Push with: git push origin $branch --tags")
  }

  private fun runInteractive() {
    val terminal = Terminal()
    val file = versionFile.get().asFile
    require(file.exists()) { "version.txt not found at ${file.path}" }

    val content = file.readText()
    val currentVersion = parseVersion(content, file.path)
    val isBeta = beta.get()
    val branch = currentBranch()

    terminal.println("Current version: $currentVersion")

    val recentTags = recentReleaseTags()
    if (recentTags.isNotEmpty()) {
      terminal.println("Recent releases: ${recentTags.joinToString(", ")}")
    }

    terminal.println("")
    runChecks(file.toRelativeString(projectDir.get().asFile), tag = null, isBeta, branch) { label, block ->
      terminal.print("  $label...")
      block()
      terminal.println(" ✔")
    }

    val bumpType = promptBumpType(terminal)
    val result = Versioning.bump(currentVersion, bumpType)
    val tag = buildTag(result.versionName, isBeta, branch)

    val existingTag = gitOutput("tag", "--list", tag)
    require(existingTag.isBlank()) { "Tag '$tag' already exists." }

    terminal.println("")
    terminal.println("  $currentVersion → ${result.versionName}  (BUILD_NUMBER = ${result.buildNumber})")
    terminal.println("")

    if (dryRun.get()) {
      val preview = previewChangelog(tag)
      if (preview.isNotBlank()) {
        terminal.println("Changelog preview:")
        terminal.println(preview)
        terminal.println("")
      }
      terminal.println("Dry run complete. No files modified, no commits or tags created.")
      return
    }

    val preview = previewChangelog(tag)
    if (preview.isNotBlank()) {
      terminal.println("Changelog preview:")
      terminal.println(preview)
      terminal.println("")
    } else {
      terminal.println("No commits since last tag.")
    }

    val confirmed = YesNoPrompt(
      prompt = "Commit version.txt, update CHANGELOG.md, and tag $tag?",
      terminal = terminal,
    ).ask() == true

    if (!confirmed) {
      terminal.println("Aborted.")
      return
    }

    writeVersionFile(file, content, result)

    val changelog = changelogFile.get().asFile
    generateChangelog(changelog, tag)
    terminal.println("Updated ${changelog.name}")

    git("add", file.absolutePath)
    git("add", changelog.absolutePath)
    git("commit", "-m", "release: $tag")
    git("tag", "-a", tag, "-m", "Release $tag")

    terminal.println("Created commit and tag: $tag")

    val pushConfirmed = YesNoPrompt(
      prompt = "Push commit and tag to origin?",
      terminal = terminal,
    ).ask() == true

    if (pushConfirmed) {
      git("push", "origin", branch, "--tags")
      terminal.println("Pushed to origin/$branch with tags.")
    } else {
      terminal.println("Skipped push. Run manually: git push origin $branch --tags")
    }
  }

  private fun printDryRun(currentVersion: String, result: Versioning.BumpResult, tag: String) {
    logger.lifecycle("$currentVersion → ${result.versionName} (BUILD_NUMBER = ${result.buildNumber})")
    logger.lifecycle("Tag: $tag")

    val preview = previewChangelog(tag)
    if (preview.isNotBlank()) {
      logger.lifecycle("Changelog preview:")
      logger.lifecycle(preview)
    }

    logger.lifecycle("Dry run complete. No files modified, no commits or tags created.")
  }

  private fun parseVersion(content: String, path: String): String {
    val match = Versioning.VERSION_REGEX.find(content)
      ?: error("VERSION_NUMBER not found in $path")
    return match.groupValues[1]
  }

  private fun writeVersionFile(file: File, content: String, result: Versioning.BumpResult) {
    require(Versioning.BUILD_REGEX.containsMatchIn(content)) {
      "BUILD_NUMBER not found in ${file.path}"
    }
    val updated = content
      .replace(Versioning.VERSION_REGEX, "VERSION_NUMBER = ${result.versionName}")
      .replace(Versioning.BUILD_REGEX, "BUILD_NUMBER = ${result.buildNumber}")
    file.writeText(updated)
  }

  private fun runChecks(
    versionFilePath: String,
    tag: String?,
    isBeta: Boolean,
    branch: String,
    reporter: (label: String, block: () -> Unit) -> Unit,
  ) {
    if (isBeta) {
      reporter("On branch '$branch'") {}
    } else {
      reporter("On main branch") {
        require(branch == "main") { "Must be on 'main' branch to release, currently on '$branch'." }
      }
    }

    reporter("Clean working tree") {
      val dirty = gitOutput("status", "--porcelain")
        .lines()
        .filter { it.isNotBlank() }
        .filter { line -> parsePorcelainPaths(line).none { it == versionFilePath } }
      require(dirty.isEmpty()) {
        "Working tree has uncommitted changes:\n${dirty.joinToString("\n")}\nCommit or stash them before releasing."
      }
    }

    reporter("Fetching remote") {
      git("fetch", "--tags")
      val fetchResult = execOperations.exec {
        it.commandLine("git", "fetch", "origin", branch)
        it.isIgnoreExitValue = true
      }
      if (fetchResult.exitValue != 0) {
        logger.lifecycle("Remote branch 'origin/$branch' not found — pushing branch to origin.")
        git("push", "-u", "origin", branch)
      }
    }

    reporter("Branch up-to-date") {
      val behind = gitOutput("rev-list", "--count", "HEAD..origin/$branch")
      require(behind == "0") { "Local branch is $behind commit(s) behind origin/$branch. Pull before releasing." }
    }

    if (tag != null) {
      val existing = gitOutput("tag", "--list", tag)
      require(existing.isBlank()) { "Tag '$tag' already exists. Choose a different version or delete the existing tag." }
    }
  }

  private fun recentReleaseTags(): List<String> =
    gitOutput("tag", "--list", "v*", "--sort=-version:refname")
      .lines()
      .filter { it.isNotBlank() }
      .take(5)

  private fun promptBumpType(terminal: Terminal): String {
    val validTypes = listOf("major", "minor", "patch")
    return terminal.prompt(
      prompt = "Bump type (major/minor/patch)",
      default = "minor",
      convert = { input: String ->
        val normalized = input.trim().lowercase()
        if (normalized in validTypes) {
          ConversionResult.Valid(normalized)
        } else {
          ConversionResult.Invalid("Must be one of: ${validTypes.joinToString()}")
        }
      },
    ) ?: "minor"
  }

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
