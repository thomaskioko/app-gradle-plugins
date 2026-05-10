package io.github.thomaskioko.codegen.processor

/**
 * Minimal source level fakes of the consumer project types the generator references.
 *
 * The processor resolves type names against whatever is on the test compilation's classpath, so
 * each test must compile alongside source files that declare the consumer's navigation
 * primitives, the Decompose `ComponentContext`, the Metro annotations, and so on. Bundling these
 * as test sources rather than depending on a real artifact keeps the test module hermetic and
 * lets the compiler type check the generated output in isolation.
 *
 * The stubs are grouped into three lists. Tests pull the list that matches the annotations they
 * exercise:
 *
 * - [baseStubs] covers every test (Decompose `ComponentContext`, `ActivityScope`, the navigation
 *   primitives, Metro annotations, `kotlinx.serialization`).
 * - [tabStubs] adds the `TabChild` type from the home navigation package, used by tab root tests.
 * - [uiStubs] adds Compose and the navigation UI primitives, used by `@ScreenUi` and `@SheetUi`
 *   tests.
 *
 * The type signatures here must match the constants in
 * `codegen/processor/src/main/kotlin/io/github/thomaskioko/codegen/processor/util/External.kt`
 * exactly. If the consumer's primitives change, both files must be updated together. End to end
 * compilation in the test suite is what catches drift.
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

        public sealed interface BaseRoute

        public interface NavRoute : BaseRoute

        public interface NavRoot : BaseRoute

        public interface RootChild

        public class ScreenDestination<out T : Any>(public val presenter: T) : RootChild

        public interface SheetChild

        public class SheetDestination<out T : Any>(public val presenter: T) : SheetChild

        public sealed class NavDestination<out R : BaseRoute>(
            public val routeClass: KClass<out R>,
        ) {
            public fun matches(route: BaseRoute): Boolean = routeClass.isInstance(route)

            public class Screen<R : NavRoute>(
                routeClass: KClass<R>,
                private val factory: (R, ComponentContext) -> RootChild,
            ) : NavDestination<R>(routeClass) {
                public fun createChild(route: BaseRoute, componentContext: ComponentContext): RootChild =
                    @Suppress("UNCHECKED_CAST") factory(route as R, componentContext)
            }

            public class Overlay<R : NavRoute>(
                routeClass: KClass<R>,
                private val factory: (R, ComponentContext) -> RootChild,
            ) : NavDestination<R>(routeClass) {
                public fun createChild(route: BaseRoute, componentContext: ComponentContext): RootChild =
                    @Suppress("UNCHECKED_CAST") factory(route as R, componentContext)
            }

            public class TabRoot<R : NavRoot>(
                routeClass: KClass<R>,
                private val factory: (R, ComponentContext) -> RootChild,
            ) : NavDestination<R>(routeClass) {
                public fun createChild(route: BaseRoute, componentContext: ComponentContext): RootChild =
                    @Suppress("UNCHECKED_CAST") factory(route as R, componentContext)
            }
        }

        public data class NavRouteBinding<T : NavRoute>(
            public val kClass: KClass<T>,
            public val serializer: KSerializer<T>,
        )

        public data class NavRootBinding<T : NavRoot>(
            public val kClass: KClass<T>,
            public val serializer: KSerializer<T>,
        )
    """.trimIndent()

    val homeNav = "HomeNav.kt" to """
        package com.thomaskioko.tvmaniac.home.nav

        import com.thomaskioko.tvmaniac.navigation.RootChild

        public class TabChild<out T : Any>(public val presenter: T) : RootChild
    """.trimIndent()

    val homeConfig = "HomeRoots.kt" to """
        package com.thomaskioko.tvmaniac.home.nav.roots

        import com.thomaskioko.tvmaniac.navigation.NavRoot
        import kotlinx.serialization.KSerializer
        import kotlinx.serialization.Serializable

        @Serializable
        public data object DiscoverRoot : NavRoot {
            public fun serializer(): KSerializer<DiscoverRoot> = error("stub")
        }

        @Serializable
        public data object LibraryRoot : NavRoot {
            public fun serializer(): KSerializer<LibraryRoot> = error("stub")
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

        @Target(AnnotationTarget.CLASS)
        public annotation class BindingContainer

        @Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
        public annotation class Provides

        @Target(AnnotationTarget.FUNCTION)
        public annotation class IntoSet

        @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
        public annotation class SingleIn(val scope: KClass<*>)
    """.trimIndent()

    val composeUi = "ComposeUi.kt" to """
        package androidx.compose.ui

        public interface Modifier {
            public companion object : Modifier
        }

        @Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.PROPERTY_GETTER)
        public annotation class Composable
    """.trimIndent()

    val composeRuntime = "ComposeRuntime.kt" to """
        package androidx.compose.runtime

        @Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.PROPERTY_GETTER)
        public annotation class Composable
    """.trimIndent()

    val navigationUi = "NavigationUi.kt" to """
        package com.thomaskioko.tvmaniac.navigation.ui

        import androidx.compose.ui.Composable
        import androidx.compose.ui.Modifier
        import com.thomaskioko.tvmaniac.navigation.RootChild
        import com.thomaskioko.tvmaniac.navigation.SheetChild

        public class ScreenContent(
            public val matches: (RootChild) -> Boolean,
            public val content: @Composable (RootChild, Modifier) -> Unit,
        )

        public class SheetContent(
            public val matches: (SheetChild) -> Boolean,
            public val content: @Composable (SheetChild) -> Unit,
        )
    """.trimIndent()

    val baseStubs: List<Pair<String, String>> = listOf(
        serialization,
        componentContext,
        activityScope,
        navRoute,
        metroAnnotations,
    )

    val tabStubs: List<Pair<String, String>> = baseStubs + listOf(homeNav, homeConfig)

    val uiStubs: List<Pair<String, String>> = baseStubs + listOf(composeUi, navigationUi)

    val tabUiStubs: List<Pair<String, String>> = uiStubs + listOf(homeNav)

    val appRootUiStubs: List<Pair<String, String>> =
        baseStubs + listOf(composeUi, composeRuntime, navigationUi)
}
