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
package io.github.thomaskioko.gradle.plugins.utils

internal object Versioning {

    internal val VERSION_REGEX: Regex = Regex("""VERSION_NUMBER\s*=\s*(\S+)""")
    internal val BUILD_REGEX: Regex = Regex("""BUILD_NUMBER\s*=\s*(\S+)""")

    internal fun compute(versionName: String): Int {
        val (major, minor, patch) = parseSemver(versionName)
        require(major in 0..209) { "Major version must be 0-209, got: $major" }
        require(minor in 0..99) { "Minor version must be 0-99, got: $minor" }
        require(patch in 0..99) { "Patch version must be 0-99, got: $patch" }
        val result = (major * 10_000_000) + (minor * 100_000) + (patch * 1_000)
        require(result in 0..Int.MAX_VALUE) { "Version code overflow: $versionName produces $result" }
        return result
    }

    internal fun bump(versionName: String, bumpType: String): String {
        val (major, minor, patch) = parseSemver(versionName)
        val (newMajor, newMinor, newPatch) = when (bumpType) {
            "major" -> Triple(major + 1, 0, 0)
            "minor" -> Triple(major, minor + 1, 0)
            "patch" -> Triple(major, minor, patch + 1)
            else -> throw IllegalArgumentException("bumpType must be major, minor, or patch, got: $bumpType")
        }
        val newVersion = "$newMajor.$newMinor.$newPatch"
        validateSemver(newVersion)
        return newVersion
    }

    private fun validateSemver(versionName: String) {
        val (major, minor, patch) = parseSemver(versionName)
        require(major in 0..209) { "Major version must be 0-209, got: $major" }
        require(minor in 0..99) { "Minor version must be 0-99, got: $minor" }
        require(patch in 0..99) { "Patch version must be 0-99, got: $patch" }
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
}
