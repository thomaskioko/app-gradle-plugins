package io.github.thomaskioko.codegen.featureflag

import com.tschuchort.compiletesting.KotlinCompilation
import io.github.thomaskioko.codegen.processor.GoldenFileAssert
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class FeatureFlagMultiFlagTest {

    @Test
    fun `should generate one binding file per FeatureFlag-decorated qualifier`() {
        val sources = FeatureFlagStubs.baseStubs.toMap() + mapOf(
            "Qualifiers.kt" to """
                package com.thomaskioko.tvmaniac.featureflags.flags

                import dev.zacsweers.metro.Qualifier
                import io.github.thomaskioko.codegen.annotations.FeatureFlag

                @Qualifier
                @FeatureFlag(
                    key = "enable_continue_watching_nitro",
                    title = "Progress Endpoint",
                    description = "Use Trakt's internal /sync/progress/up_next_nitro call instead of the documented multi-step progress fetch.",
                    defaultValue = false,
                    dateAdded = "2026-05-20",
                )
                public annotation class ContinueWatchingNitroFlagQualifier

                @Qualifier
                @FeatureFlag(
                    key = "simkl_login_enabled",
                    title = "Simkl Login",
                    description = "Show the Simkl login entry point on the settings screen.",
                    defaultValue = false,
                    dateAdded = "2026-05-17",
                )
                public annotation class SimklLoginFlagQualifier
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
            "Expected exactly 2 generated files, got ${files.keys}",
            setOf("ContinueWatchingNitroFlagBinding.kt", "SimklLoginFlagBinding.kt"),
            files.keys,
        )

        GoldenFileAssert.assertMatches(
            "featureflag/multi",
            "ContinueWatchingNitroFlagBinding.kt",
            files.getValue("ContinueWatchingNitroFlagBinding.kt"),
        )
        GoldenFileAssert.assertMatches(
            "featureflag/multi",
            "SimklLoginFlagBinding.kt",
            files.getValue("SimklLoginFlagBinding.kt"),
        )
    }
}
