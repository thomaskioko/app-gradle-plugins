package io.github.thomaskioko.codegen.featureflag

import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class FeatureFlagErrorPathTest {

    @Test
    fun `should report InvalidTarget when FeatureFlag is on an annotation class`() {
        val sources = FeatureFlagStubs.baseStubs.toMap() + mapOf(
            "AnnotationAnchor.kt" to """
                package com.thomaskioko.tvmaniac.featureflags.flags

                import io.github.thomaskioko.codegen.annotations.FeatureFlag

                @FeatureFlag(
                    key = "some_key",
                    title = "Some Title",
                    description = "Some description.",
                    defaultValue = false,
                    dateAdded = "2026-05-20",
                )
                public annotation class SomeFlagQualifier
            """.trimIndent(),
        )

        val result = FeatureFlagTestRunner().run(sources)
        assertCompilationError(result, "[FeatureFlag/InvalidTarget]")
    }

    @Test
    fun `should report EmptyKey when key is blank`() {
        val sources = FeatureFlagStubs.baseStubs.toMap() + mapOf(
            "EmptyKey.kt" to """
                package com.thomaskioko.tvmaniac.featureflags.flags

                import io.github.thomaskioko.codegen.annotations.FeatureFlag

                @FeatureFlag(
                    key = "",
                    title = "Some Title",
                    description = "Some description.",
                    defaultValue = false,
                    dateAdded = "2026-05-20",
                )
                public object EmptyKeyFlag
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

                import io.github.thomaskioko.codegen.annotations.FeatureFlag

                @FeatureFlag(
                    key = "some_key",
                    title = "",
                    description = "Some description.",
                    defaultValue = false,
                    dateAdded = "2026-05-20",
                )
                public object EmptyTitleFlag
            """.trimIndent(),
        )

        val result = FeatureFlagTestRunner().run(sources)
        assertCompilationError(result, "[FeatureFlag/EmptyTitle]")
    }

    @Test
    fun `should report EmptyDescription when description is blank`() {
        val sources = FeatureFlagStubs.baseStubs.toMap() + mapOf(
            "EmptyDescription.kt" to """
                package com.thomaskioko.tvmaniac.featureflags.flags

                import io.github.thomaskioko.codegen.annotations.FeatureFlag

                @FeatureFlag(
                    key = "some_key",
                    title = "Some Title",
                    description = "",
                    defaultValue = false,
                    dateAdded = "2026-05-20",
                )
                public object EmptyDescriptionFlag
            """.trimIndent(),
        )

        val result = FeatureFlagTestRunner().run(sources)
        assertCompilationError(result, "[FeatureFlag/EmptyDescription]")
    }

    @Test
    fun `should report InvalidDate when dateAdded is malformed`() {
        val sources = FeatureFlagStubs.baseStubs.toMap() + mapOf(
            "InvalidDate.kt" to """
                package com.thomaskioko.tvmaniac.featureflags.flags

                import io.github.thomaskioko.codegen.annotations.FeatureFlag

                @FeatureFlag(
                    key = "some_key",
                    title = "Some Title",
                    description = "Some description.",
                    defaultValue = false,
                    dateAdded = "not-a-date",
                )
                public object InvalidDateFlag
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
