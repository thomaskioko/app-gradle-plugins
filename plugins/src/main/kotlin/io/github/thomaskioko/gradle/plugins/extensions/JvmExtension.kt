package io.github.thomaskioko.gradle.plugins.extensions

import io.github.thomaskioko.gradle.plugins.setup.setupStandaloneLint
import org.gradle.api.Project

/**
 * Configures JVM-specific options on a module that has applied the `jvm` plugin.
 *
 * Reachable through `scaffold {}` once the `jvm` convention plugin is applied. Use this for pure
 * JVM modules; multiplatform modules with a JVM target use [BaseExtension.addJvmTarget] instead.
 *
 * ```kotlin
 * scaffold {
 *   useAndroidLint()
 * }
 * ```
 *
 * @property project The Gradle [Project] this extension is attached to.
 */
@ScaffoldDsl
public abstract class JvmExtension(private val project: Project) {
    /**
     * Applies the standalone Android Lint plugin (`com.android.lint`) to the JVM module.
     *
     * Use this when a JVM-only module ships custom lint rules or runs the project's lint
     * baselines without an Android target. The setup helper wires the lint plugin's reporting,
     * baseline, and `lintOptions` configuration to the project's defaults.
     *
     * ```kotlin
     * scaffold {
     *   useAndroidLint()
     * }
     * ```
     */
    public fun useAndroidLint() {
        project.plugins.apply("com.android.lint")

        project.setupStandaloneLint()
    }
}
