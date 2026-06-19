package io.github.thomaskioko.codegen.featureflag

import com.tschuchort.compiletesting.KotlinCompilation
import io.github.thomaskioko.codegen.processor.GoldenFileAssert
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the `platform`-field routing in the hermetic processor harness.
 *
 * The compile-testing run reports a single JVM platform, so the processor treats it as a JVM
 * per-target run. A `platform = Platform.JVM` flag is owned by this run and generated; an unscoped
 * (`Platform.ALL`) flag is owned by the `commonMain` metadata run and skipped here. Both anchors
 * live in `commonMain` — the field, not the source set, decides routing. The metadata-versus-native
 * split across real compilations is covered by the plugin's `FeatureFlagCodegenFunctionalTest`.
 */
@OptIn(ExperimentalCompilerApi::class)
class FeatureFlagPlatformCaseTest {

    @Test
    fun `should generate the JVM-scoped flag and skip the unscoped flag on a JVM run`() {
        val sources = FeatureFlagStubs.baseStubs.toMap() + mapOf(
            "src/commonMain/kotlin/com/thomaskioko/tvmaniac/featureflags/flags/EnableShowRatingsFlag.kt" to """
                package com.thomaskioko.tvmaniac.featureflags.flags

                import io.github.thomaskioko.codegen.annotations.FeatureFlag

                @FeatureFlag(
                    key = "enable_show_ratings",
                    title = "Show Ratings",
                    description = "Show TMDB ratings on show cards.",
                    defaultValue = false,
                    dateAdded = "2026-06-18",
                )
                public object EnableShowRatingsFlag
            """.trimIndent(),
            "src/commonMain/kotlin/com/thomaskioko/tvmaniac/featureflags/flags/EnableDynamicColorFlag.kt" to """
                package com.thomaskioko.tvmaniac.featureflags.flags

                import io.github.thomaskioko.codegen.annotations.FeatureFlag
                import io.github.thomaskioko.codegen.annotations.Platform

                @FeatureFlag(
                    key = "enable_dynamic_color",
                    title = "Dynamic Color",
                    description = "Apply the Android Material You dynamic color scheme.",
                    defaultValue = false,
                    dateAdded = "2026-06-18",
                    platform = Platform.JVM,
                )
                public object EnableDynamicColorFlag
            """.trimIndent(),
        )

        val result = FeatureFlagTestRunner().run(sources)
        assertEquals(
            "Compilation failed:\n${result.messages}",
            KotlinCompilation.ExitCode.OK,
            result.exitCode,
        )

        val files = result.generatedFiles
        assertEquals(
            "Expected only the JVM-scoped flag to generate on a JVM run, got ${files.keys}",
            setOf("EnableDynamicColorFlagQualifier.kt", "EnableDynamicColorFlagBinding.kt"),
            files.keys,
        )

        GoldenFileAssert.assertMatches(
            "featureflag/platform-case",
            "EnableDynamicColorFlagQualifier.kt",
            files.getValue("EnableDynamicColorFlagQualifier.kt"),
        )
        GoldenFileAssert.assertMatches(
            "featureflag/platform-case",
            "EnableDynamicColorFlagBinding.kt",
            files.getValue("EnableDynamicColorFlagBinding.kt"),
        )
    }
}
