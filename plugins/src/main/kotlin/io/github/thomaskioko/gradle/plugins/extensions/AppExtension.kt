package io.github.thomaskioko.gradle.plugins.extensions

import io.github.thomaskioko.gradle.plugins.utils.androidApp
import org.gradle.api.Project
import java.io.File

/**
 * Configures Android application options on a module that has applied the `app` plugin.
 *
 * Reachable through `scaffold { android { ... } }` once the `app` convention plugin has registered
 * an Android application target. Options exposed here apply to the application module only.
 * Library modules use [AndroidExtension] instead.
 *
 * ```kotlin
 * scaffold {
 *   android {
 *     applicationId("com.example.app")
 *     applicationIdSuffix(buildType = "debug", suffix = ".debug")
 *     minify(file("proguard-rules.pro"))
 *   }
 * }
 * ```
 *
 * @property project The Gradle [Project] this extension is attached to.
 */
@ScaffoldDsl
public abstract class AppExtension(private val project: Project) {
    /**
     * Sets `defaultConfig.applicationId` on the Android application target.
     *
     * The application ID is the unique identifier the OS uses for the installed app and the key
     * Google Play uses to identify a release. It does not have to match the Kotlin package name.
     *
     * ```kotlin
     * scaffold {
     *   android {
     *     applicationId("com.example.app")
     *   }
     * }
     * ```
     *
     * @param applicationId The application ID to write to `defaultConfig.applicationId`.
     */
    public fun applicationId(applicationId: String) {
        project.androidApp {
            defaultConfig.applicationId = applicationId
        }
    }

    /**
     * Sets `applicationIdSuffix` on the named build type.
     *
     * Use this to install debug and release builds side by side on a single device. A common
     * pattern is `applicationIdSuffix(buildType = "debug", suffix = ".debug")`, which produces
     * `com.example.app.debug` for debug builds and leaves the release ID untouched.
     *
     * ```kotlin
     * scaffold {
     *   android {
     *     applicationIdSuffix(buildType = "debug", suffix = ".debug")
     *   }
     * }
     * ```
     *
     * @param buildType Build type name registered on the Android application target (typically
     *   `debug` or `release`).
     * @param suffix String appended to the application ID for builds of [buildType]. Include the
     *   leading dot when one is desired in the resulting ID.
     */
    public fun applicationIdSuffix(buildType: String, suffix: String) {
        project.androidApp {
            buildTypes.getByName(buildType).applicationIdSuffix = suffix
        }
    }

    /**
     * Turns on R8 minification for the `release` build type and adds ProGuard or R8
     * configuration files.
     *
     * Sets `isMinifyEnabled = true` and appends each entry in [files] to the build type's
     * `proguardFiles`. Use this to ship a minified release artifact.
     *
     * ```kotlin
     * scaffold {
     *   android {
     *     minify(
     *       file("proguard-rules.pro"),
     *       file("proguard-rules-app.pro"),
     *     )
     *   }
     * }
     * ```
     *
     * @param files ProGuard or R8 configuration files appended to the release build type's
     *   `proguardFiles` list.
     */
    public fun minify(vararg files: File) {
        project.androidApp {
            buildTypes.getByName("release") {
                it.isMinifyEnabled = true
                it.proguardFiles += files
            }
        }
    }
}
