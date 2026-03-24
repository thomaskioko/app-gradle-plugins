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
    description = "Bumps VERSION_NUMBER (major/minor/patch) and recomputes BUILD_NUMBER in version.txt"
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

    val result = Versioning.bump(currentVersion, bumpType.get())

    require(Versioning.BUILD_REGEX.containsMatchIn(content)) {
      "BUILD_NUMBER not found in ${file.path}"
    }

    val updated = content
      .replace(Versioning.VERSION_REGEX, "VERSION_NUMBER = ${result.versionName}")
      .replace(Versioning.BUILD_REGEX, "BUILD_NUMBER = ${result.buildNumber}")
    file.writeText(updated)

    logger.lifecycle("$currentVersion -> ${result.versionName} (BUILD_NUMBER = ${result.buildNumber})")
  }
}
