package io.github.thomaskioko.codegen.annotations

import kotlin.reflect.KClass

/**
 * Marks a presenter class as a root-level navigation destination.
 *
 * The navigation codegen processor reads this annotation and generates, in the same
 * presenter module's `di/` package:
 *
 * - `<Presenter>ScreenGraph` — a `@GraphExtension(route)` exposing the presenter. The [route]
 *   class itself is used as the graph's scope marker, which keeps the scope visible from every
 *   consumer (including `commonMain` and sibling modules) without a separately generated class.
 * - `<Presenter>NavDestinationBinding` — contributes both `@IntoSet NavDestination` and
 *   `@IntoSet NavRouteBinding<*>` so polymorphic state restoration can resolve the route.
 *
 * @property route the feature's route class. Expected to implement the consumer project's
 *                 `NavRoute` interface; enforced at KSP processing time. Also used as the
 *                 generated graph's scope marker.
 * @property parentScope the parent DI scope whose factory provides a `ComponentContext`.
 *                       Typically `ActivityScope::class` in the consumer project.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class NavScreen(
    val route: KClass<*>,
    val parentScope: KClass<*>,
)
