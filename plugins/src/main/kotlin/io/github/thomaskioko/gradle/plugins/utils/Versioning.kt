package io.github.thomaskioko.gradle.plugins.utils

public object Versioning {

  public val VERSION_REGEX: Regex = Regex("""VERSION_NUMBER\s*=\s*(\S+)""")
  public val BUILD_REGEX: Regex = Regex("""BUILD_NUMBER\s*=\s*(\S+)""")

  public fun compute(versionName: String, betaNumber: Int = 0): Int {
    val (major, minor, patch) = parseSemver(versionName)
    require(major in 0..209) { "Major version must be 0-209, got: $major" }
    require(minor in 0..99) { "Minor version must be 0-99, got: $minor" }
    require(patch in 0..99) { "Patch version must be 0-99, got: $patch" }
    require(betaNumber in 0..999) { "Beta number must be 0-999, got: $betaNumber" }
    val result = (major * 10_000_000) + (minor * 100_000) + (patch * 1_000) + betaNumber
    require(result in 0..Int.MAX_VALUE) { "Version code overflow: $versionName produces $result" }
    return result
  }

  public fun bump(versionName: String, bumpType: String): BumpResult {
    val (major, minor, patch) = parseSemver(versionName)
    val (newMajor, newMinor, newPatch) = when (bumpType) {
      "major" -> Triple(major + 1, 0, 0)
      "minor" -> Triple(major, minor + 1, 0)
      "patch" -> Triple(major, minor, patch + 1)
      else -> throw IllegalArgumentException("bumpType must be major, minor, or patch, got: $bumpType")
    }
    val newVersion = "$newMajor.$newMinor.$newPatch"
    val buildNumber = compute(newVersion)
    return BumpResult(newVersion, buildNumber)
  }

  private fun parseSemver(versionName: String): Triple<Int, Int, Int> {
    val parts = versionName.split(".")
    require(parts.size == 3) { "Version must be in major.minor.patch format, got: $versionName" }
    val (major, minor, patch) = parts.map {
      it.toIntOrNull() ?: throw IllegalArgumentException(
        "Version components must be integers, got: '$it' in '$versionName'",
      )
    }
    return Triple(major, minor, patch)
  }

  public data class BumpResult(
    val versionName: String,
    val buildNumber: Int,
  )
}
