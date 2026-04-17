package io.github.thomaskioko.codegen.processor

import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class NavSheetTest {

    @Test
    fun `should generate scope graph and sheet destination binding for NavSheet`() {
        val sources = TestStubs.baseStubs.toMap() + mapOf(
            "ScreenSource.kt" to """
                package com.thomaskioko.root.model

                public enum class ScreenSource { Default, ShowDetails }
            """.trimIndent(),
            "EpisodeSheetConfig.kt" to """
                package com.thomaskioko.root.model

                import com.thomaskioko.tvmaniac.navigation.SheetConfig
                import kotlinx.serialization.KSerializer
                import kotlinx.serialization.Serializable

                @Serializable
                public data class EpisodeSheetConfig(
                    val episodeId: Long,
                    val source: ScreenSource,
                ) : SheetConfig {
                    public companion object {
                        public fun serializer(): KSerializer<EpisodeSheetConfig> = error("stub")
                    }
                }
            """.trimIndent(),
            "EpisodeDetailSheetPresenter.kt" to """
                package com.thomaskioko.tvmaniac.presentation.episodedetail

                import com.arkivanov.decompose.ComponentContext
                import com.thomaskioko.root.model.EpisodeSheetConfig
                import com.thomaskioko.root.model.ScreenSource
                import com.thomaskioko.tvmaniac.core.base.ActivityScope
                import dev.zacsweers.metro.Assisted
                import dev.zacsweers.metro.AssistedFactory
                import dev.zacsweers.metro.AssistedInject
                import io.github.thomaskioko.codegen.annotations.NavSheet

                @AssistedInject
                @NavSheet(
                    route = EpisodeSheetConfig::class,
                    parentScope = ActivityScope::class,
                )
                public class EpisodeDetailSheetPresenter(
                    @Assisted private val episodeId: Long,
                    @Assisted private val source: ScreenSource,
                    componentContext: ComponentContext,
                ) {
                    @AssistedFactory
                    public fun interface Factory {
                        public fun create(episodeId: Long, source: ScreenSource): EpisodeDetailSheetPresenter
                    }
                }
            """.trimIndent(),
        )

        val result = ProcessorTestRunner().run(sources)
        assertEquals(
            "Compilation failed:\n${result.messages}",
            KotlinCompilation.ExitCode.OK,
            result.exitCode,
        )

        val files = result.generatedFiles
        assertEquals(
            "Expected exactly 2 generated files, got ${files.keys}",
            setOf(
                "EpisodeDetailSheetScreenGraph.kt",
                "EpisodeDetailSheetDestinationBinding.kt",
            ),
            files.keys,
        )

        GoldenFileAssert.assertMatches(
            "sheet",
            "EpisodeDetailSheetScreenGraph.kt",
            files.getValue("EpisodeDetailSheetScreenGraph.kt"),
        )
        GoldenFileAssert.assertMatches(
            "sheet",
            "EpisodeDetailSheetDestinationBinding.kt",
            files.getValue("EpisodeDetailSheetDestinationBinding.kt"),
        )
    }
}
