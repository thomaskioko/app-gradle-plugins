package io.github.thomaskioko.codegen.processor

import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class NavScreenParameterizedTest {

    @Test
    fun `should generate scope graph and parameterized binding for assisted NavScreen`() {
        val sources = TestStubs.baseStubs.toMap() + mapOf(
            "ShowDetailsParam.kt" to """
                package com.thomaskioko.tvmaniac.showdetails.nav.model

                import kotlinx.serialization.Serializable

                @Serializable
                public data class ShowDetailsParam(
                    val id: Long,
                    val forceRefresh: Boolean = false,
                )
            """.trimIndent(),
            "ShowDetailsRoute.kt" to """
                package com.thomaskioko.tvmaniac.showdetails.nav

                import com.thomaskioko.tvmaniac.navigation.NavRoute
                import com.thomaskioko.tvmaniac.showdetails.nav.model.ShowDetailsParam
                import kotlinx.serialization.KSerializer
                import kotlinx.serialization.Serializable

                @Serializable
                public data class ShowDetailsRoute(public val param: ShowDetailsParam) : NavRoute {
                    public companion object {
                        public fun serializer(): KSerializer<ShowDetailsRoute> = error("stub")
                    }
                }
            """.trimIndent(),
            "ShowDetailsPresenter.kt" to """
                package com.thomaskioko.tvmaniac.presenter.showdetails

                import com.arkivanov.decompose.ComponentContext
                import com.thomaskioko.tvmaniac.core.base.ActivityScope
                import com.thomaskioko.tvmaniac.showdetails.nav.ShowDetailsRoute
                import com.thomaskioko.tvmaniac.showdetails.nav.model.ShowDetailsParam
                import dev.zacsweers.metro.Assisted
                import dev.zacsweers.metro.AssistedFactory
                import dev.zacsweers.metro.AssistedInject
                import io.github.thomaskioko.codegen.annotations.NavScreen

                @AssistedInject
                @NavScreen(route = ShowDetailsRoute::class, parentScope = ActivityScope::class)
                public class ShowDetailsPresenter(
                    componentContext: ComponentContext,
                    @Assisted private val param: ShowDetailsParam,
                ) : ComponentContext by componentContext {
                    @AssistedFactory
                    public fun interface Factory {
                        public fun create(param: ShowDetailsParam): ShowDetailsPresenter
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
                "ShowDetailsScreenGraph.kt",
                "ShowDetailsNavDestinationBinding.kt",
            ),
            files.keys,
        )

        GoldenFileAssert.assertMatches(
            "parameterized",
            "ShowDetailsScreenGraph.kt",
            files.getValue("ShowDetailsScreenGraph.kt"),
        )
        GoldenFileAssert.assertMatches(
            "parameterized",
            "ShowDetailsNavDestinationBinding.kt",
            files.getValue("ShowDetailsNavDestinationBinding.kt"),
        )
    }
}
