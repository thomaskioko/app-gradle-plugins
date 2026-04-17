package io.github.thomaskioko.codegen.processor

/**
 * Minimal hand-rolled fakes of the consumer-project types the generator references. Bundling
 * them as test sources lets the compiler type-check the generated code in isolation.
 */
internal object TestStubs {

    val serialization = "Serialization.kt" to """
        package kotlinx.serialization

        @Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
        public annotation class Serializable

        public interface KSerializer<T>
    """.trimIndent()

    val componentContext = "ComponentContext.kt" to """
        package com.arkivanov.decompose

        public interface ComponentContext
    """.trimIndent()

    val activityScope = "ActivityScope.kt" to """
        package com.thomaskioko.tvmaniac.core.base

        public abstract class ActivityScope private constructor()
    """.trimIndent()

    val navRoute = "NavRoute.kt" to """
        package com.thomaskioko.tvmaniac.navigation

        import com.arkivanov.decompose.ComponentContext
        import kotlinx.serialization.KSerializer
        import kotlin.reflect.KClass

        public interface NavRoute

        public interface RootChild

        public class ScreenDestination<out T : Any>(public val presenter: T) : RootChild

        public interface SheetChild

        public class SheetDestination<out T : Any>(public val presenter: T) : SheetChild

        public interface NavDestination {
            public fun matches(route: NavRoute): Boolean
            public fun createChild(route: NavRoute, componentContext: ComponentContext): RootChild
        }

        public data class NavRouteBinding<T : NavRoute>(
            public val kClass: KClass<T>,
            public val serializer: KSerializer<T>,
        )

        public interface SheetConfig

        public interface SheetChildFactory {
            public fun matches(config: SheetConfig): Boolean
            public fun createChild(config: SheetConfig, componentContext: ComponentContext): SheetChild
        }

        public data class SheetConfigBinding<T : SheetConfig>(
            public val kClass: KClass<T>,
            public val serializer: KSerializer<T>,
        )
    """.trimIndent()

    val homeNav = "HomeNav.kt" to """
        package com.thomaskioko.tvmaniac.home.nav

        import com.arkivanov.decompose.ComponentContext
        import com.thomaskioko.tvmaniac.home.nav.di.model.HomeConfig

        public class TabChild<out T : Any>(public val presenter: T)

        public interface TabDestination {
            public fun matches(config: HomeConfig): Boolean
            public fun createChild(config: HomeConfig, componentContext: ComponentContext): TabChild<*>
        }
    """.trimIndent()

    val homeConfig = "HomeConfig.kt" to """
        package com.thomaskioko.tvmaniac.home.nav.di.model

        import kotlinx.serialization.Serializable

        @Serializable
        public sealed interface HomeConfig {
            @Serializable
            public data object Discover : HomeConfig

            @Serializable
            public data object Library : HomeConfig
        }
    """.trimIndent()

    val metroAnnotations = "MetroAnnotations.kt" to """
        package dev.zacsweers.metro

        import kotlin.reflect.KClass

        @Target(AnnotationTarget.CLASS)
        public annotation class Inject

        @Target(AnnotationTarget.CLASS)
        public annotation class AssistedInject

        @Target(AnnotationTarget.CLASS)
        public annotation class AssistedFactory

        @Target(AnnotationTarget.VALUE_PARAMETER)
        public annotation class Assisted

        @Target(AnnotationTarget.CLASS)
        public annotation class GraphExtension(val scope: KClass<*>) {
            @Target(AnnotationTarget.CLASS)
            public annotation class Factory
        }

        @Target(AnnotationTarget.CLASS)
        public annotation class ContributesTo(val scope: KClass<*>)

        @Target(AnnotationTarget.CLASS)
        public annotation class ContributesBinding(val scope: KClass<*>)

        @Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
        public annotation class Provides

        @Target(AnnotationTarget.FUNCTION)
        public annotation class IntoSet
    """.trimIndent()

    val baseStubs: List<Pair<String, String>> = listOf(
        serialization,
        componentContext,
        activityScope,
        navRoute,
        metroAnnotations,
    )

    val tabStubs: List<Pair<String, String>> = baseStubs + listOf(homeNav, homeConfig)
}
