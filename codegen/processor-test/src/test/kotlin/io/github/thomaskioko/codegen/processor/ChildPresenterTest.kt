package io.github.thomaskioko.codegen.processor

import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that [io.github.thomaskioko.codegen.annotations.ChildPresenter] produces the expected
 * graph extension: a `@GraphExtension(scope)` interface exposing the presenter as a property, with
 * a nested `@ContributesTo(parentScope) @GraphExtension.Factory` whose factory function takes a
 * `@Provides componentContext`. The output is asserted byte-for-byte against a golden.
 *
 * Two shapes are covered:
 * - Flat: `parentScope` is a tab root (today's Discover usage), so the factory contributes to that
 *   one root and only that root's host can embed the child.
 * - Embeddable: `parentScope` is a shared ancestor scope (`ActivityScope`), so the factory
 *   contributes to a graph every screen descends from. Any host below `ActivityScope` can inject the
 *   factory and embed the component, which is what lets a reusable component live in its own module.
 *   The generator is scope-agnostic: the embeddable shape needs no generator change.
 */
@OptIn(ExperimentalCompilerApi::class)
class ChildPresenterTest {

    @Test
    fun `should generate child graph for ChildPresenter with flat scope`() {
        val sources = TestStubs.baseStubs.toMap() + mapOf(
            "ProgressChildScope.kt" to """
                package com.thomaskioko.tvmaniac.progress.nav.scope

                public abstract class ProgressChildScope private constructor()
            """.trimIndent(),
            "ProgressRoot.kt" to """
                package com.thomaskioko.tvmaniac.progress.nav

                import com.thomaskioko.tvmaniac.navigation.NavRoot
                import kotlinx.serialization.KSerializer
                import kotlinx.serialization.Serializable

                @Serializable
                public data object ProgressRoot : NavRoot {
                    public fun serializer(): KSerializer<ProgressRoot> = error("stub")
                }
            """.trimIndent(),
            "UpNextPresenter.kt" to """
                package com.thomaskioko.tvmaniac.presentation.upnext

                import com.arkivanov.decompose.ComponentContext
                import com.thomaskioko.tvmaniac.progress.nav.ProgressRoot
                import com.thomaskioko.tvmaniac.progress.nav.scope.ProgressChildScope
                import dev.zacsweers.metro.Inject
                import io.github.thomaskioko.codegen.annotations.ChildPresenter

                @Inject
                @ChildPresenter(
                    scope = ProgressChildScope::class,
                    parentScope = ProgressRoot::class,
                )
                public class UpNextPresenter(
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
            "Expected exactly 1 generated file, got ${files.keys}",
            setOf("UpNextChildGraph.kt"),
            files.keys,
        )

        val graph = files.getValue("UpNextChildGraph.kt")
        assertTrue(
            "Expected @ContributesTo(ProgressRoot::class) on the factory, got:\n$graph",
            graph.contains("@ContributesTo(ProgressRoot::class)"),
        )

        GoldenFileAssert.assertMatches("child-presenter", "UpNextChildGraph.kt", graph)
    }

    @Test
    fun `should generate child graph for embeddable ChildPresenter with shared parent scope`() {
        val sources = TestStubs.baseStubs.toMap() + mapOf(
            "FeaturedShowsComponentScope.kt" to """
                package com.thomaskioko.tvmaniac.featured.nav.scope

                public abstract class FeaturedShowsComponentScope private constructor()
            """.trimIndent(),
            "FeaturedShowsPresenter.kt" to """
                package com.thomaskioko.tvmaniac.presentation.featured

                import com.arkivanov.decompose.ComponentContext
                import com.thomaskioko.tvmaniac.core.base.ActivityScope
                import com.thomaskioko.tvmaniac.featured.nav.scope.FeaturedShowsComponentScope
                import dev.zacsweers.metro.Inject
                import io.github.thomaskioko.codegen.annotations.ChildPresenter

                @Inject
                @ChildPresenter(
                    scope = FeaturedShowsComponentScope::class,
                    parentScope = ActivityScope::class,
                )
                public class FeaturedShowsPresenter(
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
            "Expected exactly 1 generated file, got ${files.keys}",
            setOf("FeaturedShowsChildGraph.kt"),
            files.keys,
        )

        val graph = files.getValue("FeaturedShowsChildGraph.kt")
        assertTrue(
            "Expected @ContributesTo(ActivityScope::class) on the factory, got:\n$graph",
            graph.contains("@ContributesTo(ActivityScope::class)"),
        )

        GoldenFileAssert.assertMatches("child-presenter-embeddable", "FeaturedShowsChildGraph.kt", graph)
    }

    @Test
    fun `should expose the assisted factory for a parameterized ChildPresenter`() {
        val sources = TestStubs.baseStubs.toMap() + mapOf(
            "ProgressChildScope.kt" to """
                package com.thomaskioko.tvmaniac.progress.nav.scope

                public abstract class ProgressChildScope private constructor()
            """.trimIndent(),
            "ProgressRoot.kt" to """
                package com.thomaskioko.tvmaniac.progress.nav

                import com.thomaskioko.tvmaniac.navigation.NavRoot
                import kotlinx.serialization.KSerializer
                import kotlinx.serialization.Serializable

                @Serializable
                public data object ProgressRoot : NavRoot {
                    public fun serializer(): KSerializer<ProgressRoot> = error("stub")
                }
            """.trimIndent(),
            "SeasonsPresenter.kt" to """
                package com.thomaskioko.tvmaniac.presentation.seasons

                import com.arkivanov.decompose.ComponentContext
                import com.thomaskioko.tvmaniac.progress.nav.ProgressRoot
                import com.thomaskioko.tvmaniac.progress.nav.scope.ProgressChildScope
                import dev.zacsweers.metro.Assisted
                import dev.zacsweers.metro.AssistedFactory
                import dev.zacsweers.metro.AssistedInject
                import io.github.thomaskioko.codegen.annotations.ChildPresenter

                @AssistedInject
                @ChildPresenter(
                    scope = ProgressChildScope::class,
                    parentScope = ProgressRoot::class,
                )
                public class SeasonsPresenter(
                    componentContext: ComponentContext,
                    @Assisted showId: Long,
                ) : ComponentContext by componentContext {
                    @AssistedFactory
                    public fun interface Factory {
                        public fun create(showId: Long): SeasonsPresenter
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
            "Expected exactly 1 generated file, got ${files.keys}",
            setOf("SeasonsChildGraph.kt"),
            files.keys,
        )

        val graph = files.getValue("SeasonsChildGraph.kt")
        assertTrue(
            "Expected the parameterized child graph to expose the assisted factory, got:\n$graph",
            graph.contains("public val seasonsFactory: SeasonsPresenter.Factory"),
        )

        GoldenFileAssert.assertMatches("child-presenter-parameterized", "SeasonsChildGraph.kt", graph)
    }
}
