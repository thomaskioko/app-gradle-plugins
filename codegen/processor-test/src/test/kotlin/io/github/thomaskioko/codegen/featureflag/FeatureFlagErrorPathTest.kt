package io.github.thomaskioko.codegen.featureflag

import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class FeatureFlagErrorPathTest {

    @Test
    fun `should report InvalidTarget when FeatureFlag is on a non-annotation class`() {
        val sources = FeatureFlagStubs.baseStubs.toMap() + mapOf(
            "NotAnAnnotation.kt" to """
                package com.thomaskioko.tvmaniac.featureflags.flags

                import io.github.thomaskioko.codegen.annotations.FeatureFlag

                @FeatureFlag(
                    key = "some_key",
                    title = "Some Title",
                    description = "Some description.",
                    defaultValue = false,
                    dateAdded = "2026-05-20",
                )
                public class NotAnAnnotation
            """.trimIndent(),
        )

        val result = FeatureFlagTestRunner().run(sources)
        assertCompilationError(result, "[FeatureFlag/InvalidTarget]")
    }

    @Test
    fun `should report MissingQualifier when annotation class lacks @Qualifier`() {
        val sources = FeatureFlagStubs.baseStubs.toMap() + mapOf(
            "MissingQualifier.kt" to """
                package com.thomaskioko.tvmaniac.featureflags.flags

                import io.github.thomaskioko.codegen.annotations.FeatureFlag

                @FeatureFlag(
                    key = "some_key",
                    title = "Some Title",
                    description = "Some description.",
                    defaultValue = false,
                    dateAdded = "2026-05-20",
                )
                public annotation class MissingQualifierFlagQualifier
            """.trimIndent(),
        )

        val result = FeatureFlagTestRunner().run(sources)
        assertCompilationError(result, "[FeatureFlag/MissingQualifier]")
    }

    @Test
    fun `should report EmptyKey when key is blank`() {
        val sources = FeatureFlagStubs.baseStubs.toMap() + mapOf(
            "EmptyKey.kt" to """
                package com.thomaskioko.tvmaniac.featureflags.flags

                import dev.zacsweers.metro.Qualifier
                import io.github.thomaskioko.codegen.annotations.FeatureFlag

                @Qualifier
                @FeatureFlag(
                    key = "",
                    title = "Some Title",
                    description = "Some description.",
                    defaultValue = false,
                    dateAdded = "2026-05-20",
                )
                public annotation class EmptyKeyFlagQualifier
            """.trimIndent(),
        )

        val result = FeatureFlagTestRunner().run(sources)
        assertCompilationError(result, "[FeatureFlag/EmptyKey]")
    }

    @Test
    fun `should report EmptyTitle when title is blank`() {
        val sources = FeatureFlagStubs.baseStubs.toMap() + mapOf(
            "EmptyTitle.kt" to """
                package com.thomaskioko.tvmaniac.featureflags.flags

                import dev.zacsweers.metro.Qualifier
                import io.github.thomaskioko.codegen.annotations.FeatureFlag

                @Qualifier
                @FeatureFlag(
                    key = "some_key",
                    title = "",
                    description = "Some description.",
                    defaultValue = false,
                    dateAdded = "2026-05-20",
                )
                public annotation class EmptyTitleFlagQualifier
            """.trimIndent(),
        )

        val result = FeatureFlagTestRunner().run(sources)
        assertCompilationError(result, "[FeatureFlag/EmptyTitle]")
    }

    @Test
    fun `should report InvalidDate when dateAdded is malformed`() {
        val sources = FeatureFlagStubs.baseStubs.toMap() + mapOf(
            "InvalidDate.kt" to """
                package com.thomaskioko.tvmaniac.featureflags.flags

                import dev.zacsweers.metro.Qualifier
                import io.github.thomaskioko.codegen.annotations.FeatureFlag

                @Qualifier
                @FeatureFlag(
                    key = "some_key",
                    title = "Some Title",
                    description = "Some description.",
                    defaultValue = false,
                    dateAdded = "not-a-date",
                )
                public annotation class InvalidDateFlagQualifier
            """.trimIndent(),
        )

        val result = FeatureFlagTestRunner().run(sources)
        assertCompilationError(result, "[FeatureFlag/InvalidDate]")
    }

    private fun assertCompilationError(
        result: FeatureFlagTestRunner.RunResult,
        expectedMarker: String,
    ) {
        assertEquals(
            "Expected compilation failure but it succeeded.\nMessages:\n${result.messages}",
            KotlinCompilation.ExitCode.COMPILATION_ERROR,
            result.exitCode,
        )
        assertTrue(
            "Expected diagnostic containing '$expectedMarker' but messages were:\n${result.messages}",
            result.messages.contains(expectedMarker),
        )
    }
}
