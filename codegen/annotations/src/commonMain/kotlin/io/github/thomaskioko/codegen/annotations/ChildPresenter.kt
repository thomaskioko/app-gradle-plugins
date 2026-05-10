package io.github.thomaskioko.codegen.annotations

import kotlin.reflect.KClass

/**
 * Marks a presenter class as a child presenter exposed through a graph extension scoped to its
 * parent host (typically a tab pager presenter).
 *
 * Without this annotation a parent presenter that exposes child presenters has to hand-write the
 * `@GraphExtension` interface with each child as a property, plus the nested factory contributing
 * to the parent's scope. With this annotation the processor emits one `@GraphExtension` for each
 * annotated child, named `<Presenter>ChildGraph`, that the parent presenter can consume as a
 * factory.
 *
 * ## Example
 *
 * ```kotlin
 * @Inject
 * @ChildPresenter(
 *     scope = ProgressChildScope::class,
 *     parentScope = ProgressRoot::class,
 * )
 * public class UpNextPresenter(
 *     componentContext: ComponentContext,
 *     // ... deps
 * ) : ComponentContext by componentContext
 * ```
 *
 * The processor emits one file (`UpNextChildGraph.kt`) into the presenter's `<package>.di`
 * sub-package:
 *
 * ```kotlin
 * @GraphExtension(ProgressChildScope::class)
 * public interface UpNextChildGraph {
 *     public val upNextPresenter: UpNextPresenter
 *
 *     @ContributesTo(ProgressRoot::class)
 *     @GraphExtension.Factory
 *     public interface Factory {
 *         public fun createUpNextGraph(@Provides componentContext: ComponentContext): UpNextChildGraph
 *     }
 * }
 * ```
 *
 * The parent presenter consumes one factory for each child:
 *
 * ```kotlin
 * @Inject
 * public class ProgressPresenter(
 *     componentContext: ComponentContext,
 *     upNextGraphFactory: UpNextChildGraph.Factory,
 *     calendarGraphFactory: CalendarChildGraph.Factory,
 * ) : ComponentContext by componentContext {
 *     public val upNextPresenter: UpNextPresenter =
 *         upNextGraphFactory.createUpNextGraph(childContext(key = "UpNext")).upNextPresenter
 *     public val calendarPresenter: CalendarPresenter =
 *         calendarGraphFactory.createCalendarGraph(childContext(key = "Calendar")).calendarPresenter
 * }
 * ```
 *
 * ## When to use it
 *
 * Use `@ChildPresenter` when a presenter is created and owned by a parent presenter rather than
 * navigated to through a route. Tab-internal pagers, expanding-card sub-screens, and similar
 * sub-components fit this pattern. Routed destinations should use `@NavDestination` instead.
 *
 * ## Validation
 *
 * The processor reports a compile error if any of the following hold:
 *
 * - The annotated symbol is not a class.
 * - The presenter is parameterized but does not have exactly one `@Assisted` constructor parameter.
 *
 * @property scope The graph scope. Used as the marker on the generated `@GraphExtension`.
 *   Multiple `@ChildPresenter` classes may share a scope; each gets its own graph extension.
 * @property parentScope The parent dependency injection scope hosting the generated factory
 *   (typically the route class of the parent host, for example `ProgressRoot::class`).
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class ChildPresenter(
    val scope: KClass<*>,
    val parentScope: KClass<*>,
)
