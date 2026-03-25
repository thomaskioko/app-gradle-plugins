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
public abstract class BumpVersionTask : DefaultTask() {

  init {
    description = "Bumps VERSION_NUMBER (major/minor/patch/beta) and updates BUILD_NUMBER in version.txt"
    group = "versioning"
  }

  @get:Internal
  public abstract val versionFile: RegularFileProperty

  @get:Input
  public abstract val bumpType: Property<String>

  @TaskAction
  public fun bump() {
    val file = versionFile.get().asFile
    require(file.exists()) { "version.txt not found at ${file.path}" }

    val content = file.readText()
    val versionMatch = Versioning.VERSION_REGEX.find(content)
      ?: error("VERSION_NUMBER not found in ${file.path}")
    val currentVersion = versionMatch.groupValues[1]

    val buildMatch = Versioning.BUILD_REGEX.find(content)
      ?: error("BUILD_NUMBER not found in ${file.path}")
    val currentBuild = buildMatch.groupValues[1].toIntOrNull()
      ?: error("BUILD_NUMBER is not a valid integer in ${file.path}")

    val type = bumpType.get()

    if (type == "beta") {
      val baseBuild = Versioning.compute(currentVersion)
      require(currentBuild >= baseBuild) {
        "BUILD_NUMBER ($currentBuild) is less than the base for version $currentVersion ($baseBuild). " +
          "Run 'bumpVersion -Ptype=patch' to reset, or fix version.txt manually."
      }
      val newBuild = currentBuild + 1
      val maxBuild = baseBuild + 999
      require(newBuild <= maxBuild) {
        "Beta number exceeded 999 for version $currentVersion. Bump patch/minor/major to continue."
      }

      val updated = content
        .replace(Versioning.BUILD_REGEX, "BUILD_NUMBER = $newBuild")
      file.writeText(updated)

      val betaIteration = newBuild - baseBuild
      logger.lifecycle("$currentVersion beta $betaIteration (BUILD_NUMBER = $currentBuild -> $newBuild)")
    } else {
      val newVersion = Versioning.bump(currentVersion, type)
      val newBuild = Versioning.compute(newVersion)

      val updated = content
        .replace(Versioning.VERSION_REGEX, "VERSION_NUMBER = $newVersion")
        .replace(Versioning.BUILD_REGEX, "BUILD_NUMBER = $newBuild")
      file.writeText(updated)

      logger.lifecycle("$currentVersion -> $newVersion (BUILD_NUMBER = $newBuild)")
    }
  }
}
