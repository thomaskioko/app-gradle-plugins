package io.github.thomaskioko.codegen.processor

import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class ErrorPathTest {

    @Test
    fun `should report error when parameterized NavDestination has no Assisted parameter`() {
        val sources = TestStubs.baseStubs.toMap() + mapOf(
            "BadRoute.kt" to """
                package com.example.bad

                import com.thomaskioko.tvmaniac.navigation.NavRoute
                import kotlinx.serialization.KSerializer
                import kotlinx.serialization.Serializable

                @Serializable
                public data object BadRoute : NavRoute {
                    public fun serializer(): KSerializer<BadRoute> = error("stub")
                }
            """.trimIndent(),
            "BadPresenter.kt" to """
                package com.example.bad

                import com.arkivanov.decompose.ComponentContext
                import com.thomaskioko.tvmaniac.core.base.ActivityScope
                import dev.zacsweers.metro.AssistedFactory
                import dev.zacsweers.metro.AssistedInject
                import io.github.thomaskioko.codegen.annotations.DestinationKind
                import io.github.thomaskioko.codegen.annotations.NavDestination

                @AssistedInject
                @NavDestination(
                    route = BadRoute::class,
                    parentScope = ActivityScope::class,
                    kind = DestinationKind.SCREEN,
                )
                public class BadPresenter(
                    componentContext: ComponentContext,
                ) : ComponentContext by componentContext {
                    @AssistedFactory
                    public fun interface Factory {
                        public fun create(): BadPresenter
                    }
                }
            """.trimIndent(),
        )

        val result = ProcessorTestRunner().run(sources)
        assertEquals(
            "Expected compilation to fail with KSP error",
            KotlinCompilation.ExitCode.COMPILATION_ERROR,
            result.exitCode,
        )
        assertTrue(
            "Expected error mentioning @Assisted constructor parameter, got:\n${result.messages}",
            result.messages.contains("@Assisted constructor parameter"),
        )
    }

    @Test
    fun `should report error when NavDestination TAB_ROOT presenter is AssistedInject`() {
        val sources = TestStubs.tabStubs.toMap() + mapOf(
            "BadTabPresenter.kt" to """
                package com.example.tab

                import com.arkivanov.decompose.ComponentContext
                import com.thomaskioko.tvmaniac.core.base.ActivityScope
                import com.thomaskioko.tvmaniac.home.nav.roots.DiscoverRoot
                import dev.zacsweers.metro.Assisted
                import dev.zacsweers.metro.AssistedFactory
                import dev.zacsweers.metro.AssistedInject
                import io.github.thomaskioko.codegen.annotations.DestinationKind
                import io.github.thomaskioko.codegen.annotations.NavDestination

                @AssistedInject
                @NavDestination(
                    route = DiscoverRoot::class,
                    parentScope = ActivityScope::class,
                    kind = DestinationKind.TAB_ROOT,
                )
                public class BadTabPresenter(
                    componentContext: ComponentContext,
                    @Assisted private val foo: Int,
                ) {
                    @AssistedFactory
                    public fun interface Factory {
                        public fun create(foo: Int): BadTabPresenter
                    }
                }
            """.trimIndent(),
        )

        val result = ProcessorTestRunner().run(sources)
        assertEquals(
            "Expected compilation to fail with KSP error",
            KotlinCompilation.ExitCode.COMPILATION_ERROR,
            result.exitCode,
        )
        assertTrue(
            "Expected error mentioning TAB_ROOT not supported, got:\n${result.messages}",
            result.messages.contains("does not support @AssistedInject"),
        )
    }
}
