package io.github.thomaskioko.codegen.featureflag

import com.tschuchort.compiletesting.KotlinCompilation
import io.github.thomaskioko.codegen.processor.GoldenFileAssert
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class FeatureFlagSimpleTest {

    @Test
    fun `should generate qualifier and binding for one FeatureFlag-decorated anchor`() {
        val sources = FeatureFlagStubs.baseStubs.toMap() + mapOf(
            "ContinueWatchingNitroFlag.kt" to """
                package com.thomaskioko.tvmaniac.featureflags.flags

                import io.github.thomaskioko.codegen.annotations.FeatureFlag

                @FeatureFlag(
                    key = "enable_continue_watching_nitro",
                    title = "Progress Endpoint",
                    description = "Use Trakt's internal /sync/progress/up_next_nitro call instead of the documented multi-step progress fetch.",
                    defaultValue = false,
                    dateAdded = "2026-05-20",
                )
                public object ContinueWatchingNitroFlag
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
            setOf("ContinueWatchingNitroFlagQualifier.kt", "ContinueWatchingNitroFlagBinding.kt"),
            files.keys,
        )

        GoldenFileAssert.assertMatches(
            "featureflag/simple",
            "ContinueWatchingNitroFlagQualifier.kt",
            files.getValue("ContinueWatchingNitroFlagQualifier.kt"),
        )
        GoldenFileAssert.assertMatches(
            "featureflag/simple",
            "ContinueWatchingNitroFlagBinding.kt",
            files.getValue("ContinueWatchingNitroFlagBinding.kt"),
        )
    }
}
