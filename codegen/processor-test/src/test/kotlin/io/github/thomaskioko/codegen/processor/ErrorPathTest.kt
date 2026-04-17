package io.github.thomaskioko.codegen.processor

import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class ErrorPathTest {

    @Test
    fun `should report error when parameterized NavScreen has no Assisted parameter`() {
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
                import io.github.thomaskioko.codegen.annotations.NavScreen

                @AssistedInject
                @NavScreen(route = BadRoute::class, parentScope = ActivityScope::class)
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
    fun `should report error when NavSheet presenter has no AssistedFactory`() {
        val sources = TestStubs.baseStubs.toMap() + mapOf(
            "EpisodeSheetConfig.kt" to """
                package com.example.sheet

                import kotlinx.serialization.Serializable

                @Serializable
                public data class EpisodeSheetConfig(val id: Long)
            """.trimIndent(),
            "SheetFactory.kt" to """
                package com.example.sheet

                import com.arkivanov.decompose.ComponentContext
                import com.thomaskioko.tvmaniac.navigation.SheetChild

                public interface BadSheetFactory {
                    public fun createChild(config: EpisodeSheetConfig, componentContext: ComponentContext): SheetChild
                }
            """.trimIndent(),
            "BadSheetPresenter.kt" to """
                package com.example.sheet

                import com.arkivanov.decompose.ComponentContext
                import com.thomaskioko.tvmaniac.core.base.ActivityScope
                import dev.zacsweers.metro.Inject
                import io.github.thomaskioko.codegen.annotations.NavSheet

                @Inject
                @NavSheet(
                    route = EpisodeSheetConfig::class,
                    sheetChildFactory = BadSheetFactory::class,
                    parentScope = ActivityScope::class,
                )
                public class BadSheetPresenter(componentContext: ComponentContext)
            """.trimIndent(),
        )

        val result = ProcessorTestRunner().run(sources)
        assertEquals(
            "Expected compilation to fail with KSP error",
            KotlinCompilation.ExitCode.COMPILATION_ERROR,
            result.exitCode,
        )
        assertTrue(
            "Expected error mentioning nested @AssistedFactory, got:\n${result.messages}",
            result.messages.contains("nested @AssistedFactory"),
        )
    }

    @Test
    fun `should report error when TabScreen presenter is AssistedInject`() {
        val sources = TestStubs.tabStubs.toMap() + mapOf(
            "HomeScreenScope.kt" to """
                package com.example.tab

                public abstract class HomeScreenScope private constructor()
            """.trimIndent(),
            "BadTabPresenter.kt" to """
                package com.example.tab

                import com.arkivanov.decompose.ComponentContext
                import com.thomaskioko.tvmaniac.home.nav.di.model.HomeConfig
                import dev.zacsweers.metro.Assisted
                import dev.zacsweers.metro.AssistedFactory
                import dev.zacsweers.metro.AssistedInject
                import io.github.thomaskioko.codegen.annotations.TabScreen

                @AssistedInject
                @TabScreen(config = HomeConfig.Discover::class, parentScope = HomeScreenScope::class)
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
            "Expected error mentioning @AssistedInject not supported, got:\n${result.messages}",
            result.messages.contains("does not support @AssistedInject"),
        )
    }
}
