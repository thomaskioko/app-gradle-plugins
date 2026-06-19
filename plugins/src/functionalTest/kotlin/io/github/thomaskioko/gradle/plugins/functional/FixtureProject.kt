package io.github.thomaskioko.gradle.plugins.functional

import org.gradle.testkit.runner.GradleRunner
import java.io.File

internal class FixtureProject(val rootDir: File) {
    fun runner(vararg args: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(rootDir)
            .withPluginClasspath()
            .withArguments(buildList { addAll(args.asList()); add("--stacktrace") })
            .forwardOutput()

    /**
     * Runner for fixtures that consume the plugin and codegen as a composite build (`includeBuild`)
     * rather than the injected plugin classpath. Forwards the `plugins` and `codegen` build roots so
     * the fixture's settings can include them and resolve everything from source, with no published
     * version. Does not call `withPluginClasspath()`, which would put the Kotlin Gradle Plugin on
     * the classpath and clash with an included build that applies a versioned Kotlin plugin.
     */
    fun compositeRunner(vararg args: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(rootDir)
            .withArguments(
                buildList {
                    addAll(args.asList())
                    System.getProperty(PLUGINS_DIR_PROPERTY)?.let { add("-PpluginsDir=$it") }
                    System.getProperty(CODEGEN_DIR_PROPERTY)?.let { add("-PcodegenDir=$it") }
                    add("--stacktrace")
                },
            )
            .forwardOutput()

    private companion object {
        const val PLUGINS_DIR_PROPERTY = "io.github.thomaskioko.gradle.plugins.plugins.dir"
        const val CODEGEN_DIR_PROPERTY = "io.github.thomaskioko.gradle.plugins.codegen.dir"
    }
}

internal object Fixtures {
    private const val DIR_PROPERTY = "io.github.thomaskioko.gradle.plugins.fixtures.dir"

    private val rootFixturesDir: File by lazy {
        val path = System.getProperty(DIR_PROPERTY)
            ?: error(
                "System property '$DIR_PROPERTY' is not set. " +
                    "Configure it on the functionalTest Test task.",
            )
        File(path).also {
            require(it.isDirectory) { "Fixtures directory does not exist: ${it.absolutePath}" }
        }
    }

    fun extract(name: String, into: File): FixtureProject {
        val source = File(rootFixturesDir, name)
        require(source.isDirectory) {
            "Fixture not found: ${source.absolutePath}"
        }
        source.copyRecursively(into, overwrite = true)
        return FixtureProject(into)
    }
}
