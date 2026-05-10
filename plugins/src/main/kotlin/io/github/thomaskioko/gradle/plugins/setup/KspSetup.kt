package io.github.thomaskioko.gradle.plugins.setup

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Project
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Applies the KSP plugin and threads any caller-supplied processor arguments into [KspExtension].
 *
 * For Kotlin Multiplatform projects, also registers `kspCommonMainKotlinMetadata` as the canonical
 * source of truth for code generated from `commonMain` annotations: its output directory is added
 * to the `commonMain` Kotlin source set, and every Kotlin compilation task and per-target `ksp*`
 * task gains an explicit dependency on it. Without this wiring the IDE flags KSP-generated symbols
 * referenced from `commonMain` as unresolved even though Gradle compiles cleanly, and per-target
 * compilations can race the metadata task.
 */
internal fun Project.setupKsp(arguments: List<CommandLineArgumentProvider> = emptyList()) {
    plugins.apply("com.google.devtools.ksp")

    extensions.configure(KspExtension::class.java) { extension ->
        arguments.forEach {
            extension.arg(it)
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        registerCommonMainKspMetadataSource()
    }
}

/**
 * Wires `kspCommonMainKotlinMetadata` output into the `commonMain` source set and orders every
 * downstream task after it.
 *
 * Adds `build/generated/ksp/metadata/commonMain/kotlin` as a `commonMain` Kotlin source directory
 * so the IDE indexes generated symbols alongside hand-written `commonMain` code. Configures every
 * Kotlin compilation task and per-target `ksp*` task (except the metadata task itself) to depend on
 * `kspCommonMainKotlinMetadata` so Gradle never reads the generated output before it is produced.
 */
private fun Project.registerCommonMainKspMetadataSource() {
    val metadataDir = layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin")

    extensions.configure(KotlinMultiplatformExtension::class.java) { kotlin ->
        kotlin.sourceSets.named("commonMain") { sourceSet ->
            sourceSet.kotlin.srcDir(metadataDir)
        }
    }

    tasks.configureEach { task ->
        if (task.name == "kspCommonMainKotlinMetadata") return@configureEach
        if (task.name.startsWith("ksp") || task is KotlinCompilationTask<*>) {
            task.dependsOn("kspCommonMainKotlinMetadata")
        }
    }
}
