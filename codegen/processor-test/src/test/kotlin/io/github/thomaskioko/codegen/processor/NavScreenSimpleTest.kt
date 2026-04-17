package io.github.thomaskioko.codegen.processor

import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class NavScreenSimpleTest {

    @Test
    fun `should generate scope graph and binding for simple NavScreen`() {
        val sources = TestStubs.baseStubs.toMap() + mapOf(
            "DebugRoute.kt" to """
                package com.thomaskioko.tvmaniac.debug.nav

                import com.thomaskioko.tvmaniac.navigation.NavRoute
                import kotlinx.serialization.KSerializer
                import kotlinx.serialization.Serializable

                @Serializable
                public data object DebugRoute : NavRoute {
                    public fun serializer(): KSerializer<DebugRoute> = error("stub")
                }
            """.trimIndent(),
            "DebugPresenter.kt" to """
                package com.thomaskioko.tvmaniac.debug.presenter

                import com.arkivanov.decompose.ComponentContext
                import com.thomaskioko.tvmaniac.core.base.ActivityScope
                import com.thomaskioko.tvmaniac.debug.nav.DebugRoute
                import dev.zacsweers.metro.Inject
                import io.github.thomaskioko.codegen.annotations.NavScreen

                @Inject
                @NavScreen(route = DebugRoute::class, parentScope = ActivityScope::class)
                public class DebugPresenter(
                    componentContext: ComponentContext,
                ) : ComponentContext by componentContext
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
            setOf("DebugScreenGraph.kt", "DebugNavDestinationBinding.kt"),
            files.keys,
        )

        GoldenFileAssert.assertMatches("simple", "DebugScreenGraph.kt", files.getValue("DebugScreenGraph.kt"))
        GoldenFileAssert.assertMatches(
            "simple",
            "DebugNavDestinationBinding.kt",
            files.getValue("DebugNavDestinationBinding.kt"),
        )
    }
}
