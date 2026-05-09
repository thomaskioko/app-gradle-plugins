package io.github.thomaskioko.gradle.plugins

import io.github.thomaskioko.gradle.plugins.extensions.JvmExtension
import io.github.thomaskioko.gradle.plugins.setup.setupTests
import io.github.thomaskioko.gradle.plugins.utils.baseExtension
import io.github.thomaskioko.gradle.plugins.utils.disableKotlinLibraryTasks
import io.github.thomaskioko.gradle.plugins.utils.java
import io.github.thomaskioko.gradle.plugins.utils.javaTargetVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

/**
 * Configures a JVM-only Kotlin module with the project's defaults.
 *
 * Apply this plugin on a pure JVM module such as a server, a CLI, or a JVM library that does
 * not need an Android target. The plugin applies `org.jetbrains.kotlin.jvm`, chains [BasePlugin]
 * for the shared Kotlin configuration, registers the `jvm` sub-DSL
 * ([io.github.thomaskioko.gradle.plugins.extensions.JvmExtension]) on `scaffold {}`, sets the
 * Java source and target compatibility from the project's catalog, sets the `--release` flag on
 * every `JavaCompile` task, runs the project's test setup on every `Test` task, and attaches the
 * module's `test` task to the [BasePlugin.LINUX_TEST] aggregate.
 *
 * ```kotlin
 * plugins {
 *   id("io.github.thomaskioko.gradle.plugins.jvm")
 * }
 *
 * scaffold {
 *   useAndroidLint()
 * }
 * ```
 */
public abstract class JvmPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("org.jetbrains.kotlin.jvm")
        target.plugins.apply(BasePlugin::class.java)

        target.baseExtension.extensions.create("jvm", JvmExtension::class.java)

        target.java {
            sourceCompatibility = target.javaTargetVersion.get()
            targetCompatibility = target.javaTargetVersion.get()
        }

        target.tasks.withType(JavaCompile::class.java).configureEach {
            it.options.release.set(target.javaTargetVersion.get().majorVersion.toInt())
        }

        target.tasks.withType(Test::class.java).configureEach(Test::setupTests)

        target.rootProject.tasks.named(BasePlugin.LINUX_TEST).configure {
            it.dependsOn("${target.path}:test")
        }

        target.disableKotlinLibraryTasks()
    }
}
