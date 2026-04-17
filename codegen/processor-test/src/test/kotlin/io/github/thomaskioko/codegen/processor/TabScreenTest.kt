package io.github.thomaskioko.codegen.processor

import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class TabScreenTest {

    @Test
    fun `should generate tab scope graph and binding for TabScreen`() {
        val sources = TestStubs.tabStubs.toMap() + mapOf(
            "HomeScreenScope.kt" to """
                package com.thomaskioko.tvmaniac.home.nav.scope

                public abstract class HomeScreenScope private constructor()
            """.trimIndent(),
            "DiscoverShowsPresenter.kt" to """
                package com.thomaskioko.tvmaniac.discover.presenter

                import com.arkivanov.decompose.ComponentContext
                import com.thomaskioko.tvmaniac.home.nav.di.model.HomeConfig
                import com.thomaskioko.tvmaniac.home.nav.scope.HomeScreenScope
                import dev.zacsweers.metro.Inject
                import io.github.thomaskioko.codegen.annotations.TabScreen

                @Inject
                @TabScreen(config = HomeConfig.Discover::class, parentScope = HomeScreenScope::class)
                public class DiscoverShowsPresenter(
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
            setOf(
                "DiscoverShowsTabGraph.kt",
                "DiscoverShowsTabDestinationBinding.kt",
            ),
            files.keys,
        )

        GoldenFileAssert.assertMatches(
            "tab",
            "DiscoverShowsTabGraph.kt",
            files.getValue("DiscoverShowsTabGraph.kt"),
        )
        GoldenFileAssert.assertMatches(
            "tab",
            "DiscoverShowsTabDestinationBinding.kt",
            files.getValue("DiscoverShowsTabDestinationBinding.kt"),
        )
    }
}
