package io.github.thomaskioko.codegen.processor

import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that [io.github.thomaskioko.codegen.annotations.NavDestination] produces the expected
 * output for each [io.github.thomaskioko.codegen.annotations.DestinationKind]. SCREEN asserts
 * byte-for-byte against the `simple` goldens; OVERLAY asserts the `NavDestination.Overlay`
 * subclass appears in the binding; TAB_ROOT asserts the file pair exists with the expected
 * names.
 */
@OptIn(ExperimentalCompilerApi::class)
class NavDestinationTest {

    @Test
    fun `should generate screen graph and binding for NavDestination with SCREEN kind`() {
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
                import io.github.thomaskioko.codegen.annotations.DestinationKind
                import io.github.thomaskioko.codegen.annotations.NavDestination

                @Inject
                @NavDestination(
                    route = DebugRoute::class,
                    parentScope = ActivityScope::class,
                    kind = DestinationKind.SCREEN,
                )
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

    @Test
    fun `should generate Overlay subclass for NavDestination with OVERLAY kind`() {
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
                import io.github.thomaskioko.codegen.annotations.DestinationKind
                import io.github.thomaskioko.codegen.annotations.NavDestination

                @Inject
                @NavDestination(
                    route = DebugRoute::class,
                    parentScope = ActivityScope::class,
                    kind = DestinationKind.OVERLAY,
                )
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

        val binding = files.getValue("DebugNavDestinationBinding.kt")
        assertTrue(
            "Expected NavDestination.Overlay subclass, got:\n$binding",
            binding.contains("NavDestination.Overlay("),
        )
    }

    @Test
    fun `should generate parameterized binding for SCREEN with @AssistedInject`() {
        val sources = TestStubs.baseStubs.toMap() + mapOf(
            "ShowDetailsRoute.kt" to """
                package com.thomaskioko.tvmaniac.showdetails.nav

                import com.thomaskioko.tvmaniac.navigation.NavRoute
                import kotlinx.serialization.KSerializer
                import kotlinx.serialization.Serializable

                @Serializable
                public data class ShowDetailsRoute(public val param: Long) : NavRoute {
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
                import dev.zacsweers.metro.Assisted
                import dev.zacsweers.metro.AssistedFactory
                import dev.zacsweers.metro.AssistedInject
                import io.github.thomaskioko.codegen.annotations.DestinationKind
                import io.github.thomaskioko.codegen.annotations.NavDestination

                @AssistedInject
                @NavDestination(
                    route = ShowDetailsRoute::class,
                    parentScope = ActivityScope::class,
                    kind = DestinationKind.SCREEN,
                )
                public class ShowDetailsPresenter(
                    @Assisted public val param: Long,
                    componentContext: ComponentContext,
                ) : ComponentContext by componentContext {
                    @AssistedFactory
                    public interface Factory {
                        public fun create(param: Long): ShowDetailsPresenter
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
            setOf("ShowDetailsScreenGraph.kt", "ShowDetailsNavDestinationBinding.kt"),
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

    @Test
    fun `should generate tab graph and binding for NavDestination with TAB_ROOT kind`() {
        val sources = TestStubs.tabStubs.toMap() + mapOf(
            "DiscoverPresenter.kt" to """
                package com.thomaskioko.tvmaniac.discover.presenter

                import com.arkivanov.decompose.ComponentContext
                import com.thomaskioko.tvmaniac.core.base.ActivityScope
                import com.thomaskioko.tvmaniac.home.nav.roots.DiscoverRoot
                import dev.zacsweers.metro.Inject
                import io.github.thomaskioko.codegen.annotations.DestinationKind
                import io.github.thomaskioko.codegen.annotations.NavDestination

                @Inject
                @NavDestination(
                    route = DiscoverRoot::class,
                    parentScope = ActivityScope::class,
                    kind = DestinationKind.TAB_ROOT,
                )
                public class DiscoverPresenter(
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
            setOf("DiscoverTabGraph.kt", "DiscoverTabDestinationBinding.kt"),
            files.keys,
        )
    }
}
