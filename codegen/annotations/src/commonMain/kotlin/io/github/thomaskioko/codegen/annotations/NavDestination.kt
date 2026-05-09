package io.github.thomaskioko.codegen.annotations

import kotlin.reflect.KClass

/**
 * Marks a presenter class as a navigation destination so the codegen processor can wire it into the
 * consumer's navigation graph at compile time.
 *
 * Without this annotation a presenter is just a class. The consumer would have to manually write a
 * Metro `@GraphExtension` for it, manually register a `NavDestination` factory, manually register a
 * `NavRouteBinding` for polymorphic save and restore, and repeat that boilerplate for every
 * screen, overlay, and tab in the app. With this annotation the processor emits all of that for
 * you in a file that lives next to your presenter and that your IDE can navigate to.
 *
 * The [kind] parameter picks the destination role (a regular stack screen, a modal overlay, or a
 * top level tab anchor) and the processor emits the matching pair of files.
 *
 * ## Example
 *
 * ```kotlin
 * @Inject
 * @NavDestination(
 *     route = ShowsRoute::class,
 *     parentScope = ActivityScope::class,
 *     kind = DestinationKind.SCREEN,
 * )
 * class ShowsPresenter(
 *     componentContext: ComponentContext,
 * ) : ComponentContext by componentContext
 * ```
 *
 * The processor emits two files into the presenter's `<package>.di` package: a Metro graph
 * extension named `ShowsScreenGraph` and a binding named `ShowsNavDestinationBinding`. The
 * binding contributes the presenter to the consumer's `Set<NavDestination<*>>` and
 * `Set<NavRouteBinding<*>>` multibindings.
 *
 * ## Generated artifacts by kind
 *
 * - [DestinationKind.SCREEN] emits `<Presenter>ScreenGraph` (a `@GraphExtension(route)` interface)
 *   and `<Presenter>NavDestinationBinding` contributing `@IntoSet NavDestination.Screen` plus
 *   `@IntoSet NavRouteBinding<*>`.
 * - [DestinationKind.OVERLAY] emits the same files as [DestinationKind.SCREEN] except the
 *   destination is `NavDestination.Overlay`. The consumer's navigator decides at runtime whether
 *   to push the destination onto the back stack or present it as an overlay, based on the
 *   destination subclass and the route's type.
 * - [DestinationKind.TAB_ROOT] emits `<Presenter>TabGraph` (a `@GraphExtension(route)` interface)
 *   and `<Presenter>TabDestinationBinding` contributing `@IntoSet NavDestination.TabRoot` plus
 *   `@IntoSet NavRootBinding<*>`. The route is a `NavRoot` (typically a `data object`) and doubles
 *   as the graph scope.
 *
 * ## Parameterized presenters
 *
 * If the presenter uses Metro's `@AssistedInject` with a nested `@AssistedFactory`, the processor
 * detects this and emits a parameterized binding that extracts the runtime parameter from the
 * route. The presenter's single `@Assisted` constructor parameter must have the same type as a
 * property on the route class. The processor reads the route property at navigation time and
 * passes it through the assisted factory.
 *
 * ```kotlin
 * @AssistedInject
 * @NavDestination(
 *     route = ShowDetailsRoute::class,
 *     parentScope = ActivityScope::class,
 *     kind = DestinationKind.SCREEN,
 * )
 * class ShowDetailsPresenter(
 *     @Assisted val showId: Long,
 *     componentContext: ComponentContext,
 * ) : ComponentContext by componentContext {
 *     @AssistedFactory
 *     interface Factory {
 *         fun create(showId: Long): ShowDetailsPresenter
 *     }
 * }
 *
 * @Serializable
 * data class ShowDetailsRoute(val showId: Long) : NavRoute
 * ```
 *
 * ## Validation
 *
 * The processor reports a compile error if any of the following hold:
 *
 * - The annotated symbol is not a class.
 * - [kind] is not `SCREEN`, `OVERLAY`, or `TAB_ROOT`.
 * - The presenter is parameterized but does not have exactly one `@Assisted` constructor parameter.
 * - [kind] is `TAB_ROOT` and the presenter declares a nested `@AssistedFactory`. Tab roots must use
 *   plain `@Inject` because their route is a singleton `data object` and carries no runtime payload.
 *
 * @property route The feature's route class. For [DestinationKind.SCREEN] or
 *   [DestinationKind.OVERLAY] it implements `NavRoute`. For [DestinationKind.TAB_ROOT] it
 *   implements `NavRoot`. The route class doubles as the generated graph's scope marker.
 * @property parentScope The parent dependency injection scope hosting the generated binding.
 *   Typically `ActivityScope::class` in the consumer project.
 * @property kind The destination role. See [DestinationKind].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class NavDestination(
    val route: KClass<*>,
    val parentScope: KClass<*>,
    val kind: DestinationKind,
)

/**
 * The role a [NavDestination] plays at runtime. The processor uses this value to choose which pair
 * of files to emit and which `NavDestination` subclass the generated binding contributes to.
 */
public enum class DestinationKind {
    /**
     * A screen pushed onto the navigation back stack. Tapping the system back button pops it off
     * and returns to the previous destination.
     *
     * The generated binding contributes one `NavDestination.Screen` and one `NavRouteBinding<*>`.
     * Use this for the common case of a screen that lives inside a Decompose stack and gets
     * pushed on top of the previous one.
     */
    SCREEN,

    /**
     * A modal overlay that appears on top of the current screen without affecting the back stack.
     * Examples include bottom sheets, dialogs, and menus. Dismissing the overlay returns to the
     * screen that was visible underneath.
     *
     * The generated binding contributes one `NavDestination.Overlay` and one `NavRouteBinding<*>`.
     * The output is structurally identical to [SCREEN] except for the destination subclass. The
     * consumer's navigator inspects that subclass at runtime to decide whether to push the
     * destination onto the back stack or display it in an overlay slot.
     */
    OVERLAY,

    /**
     * The destination shown when the user selects a top level tab in a bottom navigation bar.
     * Each tab anchors its own back stack and persists across tab switches.
     *
     * The generated binding contributes one `NavDestination.TabRoot` and one `NavRootBinding<*>`.
     * The route must be a `NavRoot` (typically a `data object`). Tab presenters must use plain
     * `@Inject` because the route is a singleton and carries no payload. Declaring a nested
     * `@AssistedFactory` on a tab presenter is a compile error.
     */
    TAB_ROOT,
}
