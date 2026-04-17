package io.github.thomaskioko.codegen.annotations

import kotlin.reflect.KClass

/**
 * Marks a presenter class as a modal sheet destination.
 *
 * The navigation codegen processor reads this annotation and generates, in the same presenter
 * module's `di/` package:
 *
 * - `<Presenter>ScreenGraph` — a `@GraphExtension(route)` exposing the presenter's factory. The
 *   [route] class (a `SheetConfig` subtype in the consumer project) is used as the graph's scope
 *   marker, which keeps the scope visible from every consumer (including `commonMain` and sibling
 *   modules) without a separately generated class.
 * - `<Presenter>DestinationBinding` — a `@ContributesTo(parentScope)` interface whose companion
 *   object contributes both `@IntoSet SheetChildFactory` and `@IntoSet SheetConfigBinding<*>`.
 *   This matches the stack-side `NavDestination` + `NavRouteBinding` pair: the factory handles
 *   runtime dispatch and the config binding feeds the polymorphic `KSerializer<SheetConfig>` used
 *   by Decompose's `childSlot`.
 *
 * Sheet presenters must be `@AssistedInject` with one or more `@Assisted` constructor parameters.
 * The route class names a property per `@Assisted` parameter (matching by type); the generated
 * `createChild` casts the incoming `SheetConfig` to [route] and forwards the mapped properties to
 * the presenter's assisted factory.
 *
 * @property route the sheet config class (a consumer-project `SheetConfig` subtype). Properties on
 *                 the config provide values for the presenter's `@Assisted` parameters. Also used
 *                 as the generated graph's scope marker.
 * @property parentScope the parent DI scope hosting the generated binding. Typically
 *                       `ActivityScope::class` in the consumer project.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class NavSheet(
    val route: KClass<*>,
    val parentScope: KClass<*>,
)
