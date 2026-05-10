# Examples

Concrete input and generated output for every annotation variant. Each example is taken from the
golden fixtures used by `codegen-processor-test`, so the output matches what the processor
produces in a real build.

Each section shows the annotated declaration the consumer writes, then every Kotlin file the
processor writes to disk for that input. Generated files land in a `<package>.di` sub-package
next to the annotated symbol. For how each file is built from the input, see
[architecture/generators.md](architecture/generators.md) and
[architecture/parsers.md](architecture/parsers.md).

## Contents

1. [`@NavDestination(kind = SCREEN)`, presenter with no runtime parameters](#1-navdestinationkind--screen-presenter-with-no-runtime-parameters)
2. [`@NavDestination(kind = SCREEN)`, parameterized presenter](#2-navdestinationkind--screen-parameterized-presenter)
3. [`@NavDestination(kind = OVERLAY)`](#3-navdestinationkind--overlay)
4. [`@NavDestination(kind = TAB_ROOT)`](#4-navdestinationkind--tab_root)
5. [`@ScreenUi`](#5-screenui)
6. [`@SheetUi`](#6-sheetui)
7. [`@TabUi`](#7-tabui)
8. [`@ChildPresenter`](#8-childpresenter)
9. [`@AppRoot`](#9-approot)
10. [`@AppRootUi`](#10-approotui)

## 1. `@NavDestination(kind = SCREEN)`, presenter with no runtime parameters

Use `@NavDestination(kind = SCREEN)` on a presenter declared with a plain `@Inject` constructor when the presenter needs no runtime parameters from the route. Every
dependency the presenter takes is provided by Metro from the surrounding dependency graph. `@NavDestination` generates a graph that exposes the presenter instance
directly, plus a `NavDestination.Screen` factory that builds it from a `ComponentContext` alone.

This is the simpler of the two SCREEN forms. The other (next section) covers presenters that take one runtime parameter through `@AssistedInject`.

### Input

```kotlin
package com.thomaskioko.tvmaniac.debug.presenter

@Inject
@NavDestination(
    route = DebugRoute::class,
    parentScope = ActivityScope::class,
    kind = DestinationKind.SCREEN,
)
public class DebugPresenter(
    componentContext: ComponentContext,
    private val navigator: Navigator,
    // ... more deps
) : ComponentContext by componentContext
```

### Generated: `DebugScreenGraph.kt`

```kotlin
package com.thomaskioko.tvmaniac.debug.presenter.di

@GraphExtension(DebugRoute::class)
public interface DebugScreenGraph {
    public val debugPresenter: DebugPresenter

    @ContributesTo(ActivityScope::class)
    @GraphExtension.Factory
    public interface Factory {
        public fun createDebugGraph(@Provides componentContext: ComponentContext): DebugScreenGraph
    }
}
```

### Generated: `DebugNavDestinationBinding.kt`

```kotlin
package com.thomaskioko.tvmaniac.debug.presenter.di

@ContributesTo(ActivityScope::class)
public interface DebugNavDestinationBinding {
    public companion object {
        @Provides
        @IntoSet
        public fun provideDebugNavDestination(graphFactory: DebugScreenGraph.Factory): NavDestination<*> = NavDestination.Screen(
            routeClass = DebugRoute::class,
        ) { _, componentContext ->
            ScreenDestination(graphFactory.createDebugGraph(componentContext).debugPresenter)
        }

        @Provides
        @IntoSet
        public fun provideDebugRouteBinding(): NavRouteBinding<*> =
            NavRouteBinding(DebugRoute::class, DebugRoute.serializer())
    }
}
```

## 2. `@NavDestination(kind = SCREEN)`, parameterized presenter

Use `@NavDestination(kind = SCREEN)` on a presenter declared with `@AssistedInject` and a nested `@AssistedFactory` when the presenter needs one runtime value supplied
through the route, such as a show ID or an episode ID that identifies the specific instance of the screen. The runtime value lives as a property on the route class.

The difference from the previous section is the presenter's constructor: `@AssistedInject` plus an `@Assisted` parameter on one constructor argument and a nested
`@AssistedFactory` interface. `@NavDestination` detects the assisted factory automatically, exposes it on the generated graph in place of the presenter, and generates a
factory lambda that reads the matching property off the incoming route and threads it through `factory.create(...)`.

### Input

```kotlin
package com.thomaskioko.tvmaniac.presenter.showdetails

@AssistedInject
@NavDestination(
    route = ShowDetailsRoute::class,
    parentScope = ActivityScope::class,
    kind = DestinationKind.SCREEN,
)
public class ShowDetailsPresenter(
    @Assisted private val param: ShowDetailsParam,
    componentContext: ComponentContext,
    // ... more deps
) {
    @AssistedFactory
    public fun interface Factory {
        public fun create(param: ShowDetailsParam): ShowDetailsPresenter
    }
}
```

Where `ShowDetailsRoute` is:

```kotlin
@Serializable
public data class ShowDetailsRoute(public val param: ShowDetailsParam) : NavRoute
```

### Generated: `ShowDetailsScreenGraph.kt`

```kotlin
@GraphExtension(ShowDetailsRoute::class)
public interface ShowDetailsScreenGraph {
    public val showDetailsFactory: ShowDetailsPresenter.Factory

    @ContributesTo(ActivityScope::class)
    @GraphExtension.Factory
    public interface Factory {
        public fun createShowDetailsGraph(@Provides componentContext: ComponentContext): ShowDetailsScreenGraph
    }
}
```

### Generated: `ShowDetailsNavDestinationBinding.kt`

```kotlin
@ContributesTo(ActivityScope::class)
public interface ShowDetailsNavDestinationBinding {
    public companion object {
        @Provides
        @IntoSet
        public fun provideShowDetailsNavDestination(
            graphFactory: ShowDetailsScreenGraph.Factory,
        ): NavDestination<*> = NavDestination.Screen(
            routeClass = ShowDetailsRoute::class,
        ) { showDetailsRoute, componentContext ->
            val graph = graphFactory.createShowDetailsGraph(componentContext)
            ScreenDestination(graph.showDetailsFactory.create(showDetailsRoute.param))
        }

        @Provides
        @IntoSet
        public fun provideShowDetailsRouteBinding(): NavRouteBinding<*> =
            NavRouteBinding(ShowDetailsRoute::class, ShowDetailsRoute.serializer())
    }
}
```

### Route and factory rules

If you violate either of these rules the processor reports a compile error pointing at the offending declaration. The rules let the processor match the route property to
the assisted factory parameter.

- The presenter must have exactly one `@Assisted` constructor parameter.
- The route class must have exactly one property whose type matches the assisted parameter's type.

## 3. `@NavDestination(kind = OVERLAY)`

Use `@NavDestination(kind = OVERLAY)` for a modal destination presented on top of the current screen through Decompose's slot mechanism, such as a bottom sheet, dialog,
or menu. The difference from a SCREEN destination is twofold: the route must implement `NavRoute` plus a marker interface (in Tv Maniac, `OverlayRoute`) that tells the
consumer's navigator to route the destination into the overlay slot instead of pushing it onto the back stack, and the generated binding contributes a
`NavDestination.Overlay` instead of a `NavDestination.Screen`.

`@NavDestination(kind = OVERLAY)` works with both plain `@Inject` and `@AssistedInject` presenters; the example below uses the parameterized form. The full runtime flow
lives in [architecture/consumer-contract.md](architecture/consumer-contract.md#runtime-flow).

### Input

```kotlin
package com.thomaskioko.tvmaniac.presentation.episodedetail

@AssistedInject
@NavDestination(
    route = EpisodeSheetRoute::class,
    parentScope = ActivityScope::class,
    kind = DestinationKind.OVERLAY,
)
public class EpisodeSheetPresenter(
    @Assisted private val param: EpisodeSheetParam,
    componentContext: ComponentContext,
    // ... deps
) {
    @AssistedFactory
    public fun interface Factory {
        public fun create(param: EpisodeSheetParam): EpisodeSheetPresenter
    }
}
```

Where `EpisodeSheetRoute` is:

```kotlin
@Serializable
public data class EpisodeSheetRoute(public val param: EpisodeSheetParam) : NavRoute, OverlayRoute
```

### Generated: `EpisodeSheetScreenGraph.kt`

```kotlin
@GraphExtension(EpisodeSheetRoute::class)
public interface EpisodeSheetScreenGraph {
    public val episodeSheetFactory: EpisodeSheetPresenter.Factory

    @ContributesTo(ActivityScope::class)
    @GraphExtension.Factory
    public interface Factory {
        public fun createEpisodeSheetGraph(@Provides componentContext: ComponentContext): EpisodeSheetScreenGraph
    }
}
```

### Generated: `EpisodeSheetNavDestinationBinding.kt`

```kotlin
@ContributesTo(ActivityScope::class)
public interface EpisodeSheetNavDestinationBinding {
    public companion object {
        @Provides
        @IntoSet
        public fun provideEpisodeSheetNavDestination(
            graphFactory: EpisodeSheetScreenGraph.Factory,
        ): NavDestination<*> = NavDestination.Overlay(
            routeClass = EpisodeSheetRoute::class,
        ) { episodeSheetRoute, componentContext ->
            val graph = graphFactory.createEpisodeSheetGraph(componentContext)
            ScreenDestination(graph.episodeSheetFactory.create(episodeSheetRoute.param))
        }

        @Provides
        @IntoSet
        public fun provideEpisodeSheetRouteBinding(): NavRouteBinding<*> =
            NavRouteBinding(EpisodeSheetRoute::class, EpisodeSheetRoute.serializer())
    }
}
```

The graph file is identical in form to example 2's `ShowDetailsScreenGraph`. The only difference between SCREEN and OVERLAY output is that the binding contributes a
`NavDestination.Overlay` instead of a `NavDestination.Screen`. The consumer's navigator inspects that subclass at runtime to decide whether to push the destination onto
the back stack or render it in Decompose's overlay slot.

## 4. `@NavDestination(kind = TAB_ROOT)`

Use `@NavDestination(kind = TAB_ROOT)` for the destination shown when the user selects one of the bottom navigation tabs. The difference from SCREEN and OVERLAY
destinations is the route type: a tab root's route is a `NavRoot` `data object` rather than a `NavRoute` `data class`, so the route carries no payload. Tab presenters
therefore use plain `@Inject` only; `@NavDestination` reports a compile error if a tab presenter declares a nested `@AssistedFactory`.

The generated binding contributes a `NavDestination.TabRoot` (instead of `Screen` or `Overlay`) plus a `NavRootBinding<*>` (instead of `NavRouteBinding<*>`) so the tab
root participates in polymorphic save and restore alongside the other tabs. It also contributes the route singleton itself into `Set<NavRoot>`, replacing the
hand-written `<Feature>RootBinding` files consumers used to keep next to each tab.

### Input

```kotlin
package com.thomaskioko.tvmaniac.discover.presenter

@Inject
@NavDestination(
    route = DiscoverRoot::class,
    parentScope = ActivityScope::class,
    kind = DestinationKind.TAB_ROOT,
)
public class DiscoverShowsPresenter(
    componentContext: ComponentContext,
    // ... deps
) : ComponentContext by componentContext
```

Where `DiscoverRoot` is:

```kotlin
@Serializable
public data object DiscoverRoot : NavRoot
```

### Generated: `DiscoverShowsTabGraph.kt`

```kotlin
@GraphExtension(DiscoverRoot::class)
public interface DiscoverShowsTabGraph {
    public val discoverShowsPresenter: DiscoverShowsPresenter

    @ContributesTo(ActivityScope::class)
    @GraphExtension.Factory
    public interface Factory {
        public fun createDiscoverShowsTabGraph(@Provides componentContext: ComponentContext): DiscoverShowsTabGraph
    }
}
```

### Generated: `DiscoverShowsTabDestinationBinding.kt`

```kotlin
@ContributesTo(ActivityScope::class)
public interface DiscoverShowsTabDestinationBinding {
    public companion object {
        @Provides
        @IntoSet
        public fun provideDiscoverShowsNavDestination(
            graphFactory: DiscoverShowsTabGraph.Factory,
        ): NavDestination<*> = NavDestination.TabRoot(
            routeClass = DiscoverRoot::class,
        ) { _, componentContext ->
            TabChild(graphFactory.createDiscoverShowsTabGraph(componentContext).discoverShowsPresenter)
        }

        @Provides
        @IntoSet
        public fun provideDiscoverShowsNavRoot(): NavRoot = DiscoverRoot

        @Provides
        @IntoSet
        public fun provideDiscoverShowsRootBinding(): NavRootBinding<*> =
            NavRootBinding(DiscoverRoot::class, DiscoverRoot.serializer())
    }
}
```

The tab graph is contributed to `parentScope` (typically `ActivityScope`), the same scope as the unified `Set<NavDestination<*>>`. The consumer's home presenter filters
by the `TabRoot` subclass and renders the active root. The third contribution feeds `Set<NavRoot>`, which a navigator typically iterates to enumerate the available
tabs without inspecting destination factories.

## 5. `@ScreenUi`

Use `@ScreenUi` on the Android `@Composable` function that renders a screen presenter. The annotation generates a `ScreenContent` binding that joins the composable to the
`Set<ScreenContent>` multibinding the navigation host iterates to pick the right renderer for the active screen. `@ScreenUi` replaces the mechanical binding file each
composable would otherwise need.

The previous four sections cover the presenter-side annotation. `@ScreenUi` covers the Android UI-side annotation that pairs with a `kind = SCREEN` presenter at runtime.
The next section covers the overlay equivalent, `@SheetUi`.

### Input

```kotlin
package com.thomaskioko.tvmaniac.debug.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thomaskioko.tvmaniac.core.base.ActivityScope
import com.thomaskioko.tvmaniac.debug.presenter.DebugPresenter
import io.github.thomaskioko.codegen.annotations.ScreenUi

@ScreenUi(presenter = DebugPresenter::class, parentScope = ActivityScope::class)
@Composable
public fun DebugMenuScreen(
    presenter: DebugPresenter,
    modifier: Modifier = Modifier,
) {
    // ...
}
```

### Generated: `DebugMenuScreenUiBinding.kt`

```kotlin
package com.thomaskioko.tvmaniac.debug.ui.di

import com.thomaskioko.tvmaniac.core.base.ActivityScope
import com.thomaskioko.tvmaniac.debug.presenter.DebugPresenter
import com.thomaskioko.tvmaniac.debug.ui.DebugMenuScreen
import com.thomaskioko.tvmaniac.navigation.ScreenDestination
import com.thomaskioko.tvmaniac.navigation.ui.ScreenContent
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@BindingContainer
@ContributesTo(ActivityScope::class)
public object DebugMenuScreenUiBinding {
    @Provides
    @IntoSet
    public fun provideDebugMenuScreenContent(): ScreenContent = ScreenContent(
        matches = { (it as? ScreenDestination<*>)?.presenter is DebugPresenter },
        content = { child, modifier ->
            DebugMenuScreen(
                presenter = (child as ScreenDestination<*>).presenter as DebugPresenter,
                modifier = modifier,
            )
        },
    )
}
```

The `@BindingContainer object` structure rather than `interface + companion object` is deliberate. The Android only `ui` source set does not pick up `@Provides @IntoSet`
declarations from a companion object the way the shared Kotlin Multiplatform source set does, so emitting the interface form would silently produce an empty multibinding
at runtime. The full reasoning lives in
[architecture/generators.md](architecture/generators.md#binding-container-object-for-ui-bindings-interface-companion-for-destination-bindings).

### Function signature requirement

The annotated function must accept exactly two parameters: `presenter: <PresenterType>` first and `modifier: Modifier = Modifier` second. The generator calls them by
name, so renaming either causes the generated code to fail at the next compile.

## 6. `@SheetUi`

Use `@SheetUi` on the Android `@Composable` function that renders an overlay presenter. The difference from `@ScreenUi` is the multibinding the generated code contributes
to: `@SheetUi` adds a `SheetContent` into `Set<SheetContent>` because the consumer's overlay slot iterates the sheet set, while `@ScreenUi` adds a `ScreenContent` into
`Set<ScreenContent>` because the navigation stack iterates the screen set. `@SheetUi` also does not forward `Modifier` to the composable; `@ScreenUi` does. The reason is
at the end of this section.

### Input

```kotlin
package com.thomaskioko.tvmaniac.episodedetail.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thomaskioko.tvmaniac.core.base.ActivityScope
import com.thomaskioko.tvmaniac.presentation.episodedetail.EpisodeSheetPresenter
import io.github.thomaskioko.codegen.annotations.SheetUi

@SheetUi(presenter = EpisodeSheetPresenter::class, parentScope = ActivityScope::class)
@Composable
public fun EpisodeSheet(
    presenter: EpisodeSheetPresenter,
    modifier: Modifier = Modifier,
) {
    // ...
}
```

### Generated: `EpisodeSheetUiBinding.kt`

```kotlin
package com.thomaskioko.tvmaniac.episodedetail.ui.di

import com.thomaskioko.tvmaniac.core.base.ActivityScope
import com.thomaskioko.tvmaniac.episodedetail.ui.EpisodeSheet
import com.thomaskioko.tvmaniac.navigation.SheetDestination
import com.thomaskioko.tvmaniac.navigation.ui.SheetContent
import com.thomaskioko.tvmaniac.presentation.episodedetail.EpisodeSheetPresenter
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@BindingContainer
@ContributesTo(ActivityScope::class)
public object EpisodeSheetUiBinding {
    @Provides
    @IntoSet
    public fun provideEpisodeSheetContent(): SheetContent = SheetContent(
        matches = { (it as? SheetDestination<*>)?.presenter is EpisodeSheetPresenter },
        content = { child ->
            EpisodeSheet(
                presenter = (child as SheetDestination<*>).presenter as EpisodeSheetPresenter,
            )
        },
    )
}
```

The overlay renderer does not receive a modifier. `SheetContent.content` is typed as `@Composable (SheetChild) -> Unit`. Modal layout decisions (a `ModalBottomSheet`, for
example) belong inside the composable body, not at the call site. The annotated function still accepts a `modifier: Modifier = Modifier` parameter for consistency with
other composables, but the generator does not forward it.

## 7. `@TabUi`

Use `@TabUi` on the Android `@Composable` function that renders one tab pager page. The annotation generates a `ScreenContent` binding identical in shape to the
`@ScreenUi` output, but the generated predicate matches `TabChild<*>` rather than `ScreenDestination<*>`. Use it on the four bottom-bar tab pages (Discover, Library,
Progress, Profile) where the active child is a `TabChild`-wrapped tab presenter rather than a `ScreenDestination`-wrapped routed screen.

The previous section covered the overlay renderer (`@SheetUi`). The next section covers the parent-owned child presenter (`@ChildPresenter`). The next two cover the
application root pair (`@AppRoot` and `@AppRootUi`).

### Input

```kotlin
package com.thomaskioko.tvmaniac.discover.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thomaskioko.tvmaniac.core.base.ActivityScope
import com.thomaskioko.tvmaniac.discover.presenter.DiscoverShowsPresenter
import io.github.thomaskioko.codegen.annotations.TabUi

@TabUi(presenter = DiscoverShowsPresenter::class, parentScope = ActivityScope::class)
@Composable
public fun DiscoverScreen(
    presenter: DiscoverShowsPresenter,
    modifier: Modifier = Modifier,
) {
    // ...
}
```

### Generated: `DiscoverScreenUiBinding.kt`

```kotlin
package com.thomaskioko.tvmaniac.discover.ui.di

import com.thomaskioko.tvmaniac.core.base.ActivityScope
import com.thomaskioko.tvmaniac.discover.presenter.DiscoverShowsPresenter
import com.thomaskioko.tvmaniac.discover.ui.DiscoverScreen
import com.thomaskioko.tvmaniac.home.nav.TabChild
import com.thomaskioko.tvmaniac.navigation.ui.ScreenContent
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@BindingContainer
@ContributesTo(ActivityScope::class)
public object DiscoverScreenUiBinding {
    @Provides
    @IntoSet
    public fun provideDiscoverScreenContent(): ScreenContent = ScreenContent(
        matches = { (it as? TabChild<*>)?.presenter is DiscoverShowsPresenter },
        content = { child, modifier ->
            DiscoverScreen(
                presenter = (child as TabChild<*>).presenter as DiscoverShowsPresenter,
                modifier = modifier,
            )
        },
    )
}
```

The output joins the same `Set<ScreenContent>` multibinding the navigation host iterates. The host treats `TabChild` and `ScreenDestination` the same way: it walks the
set, finds the entry whose predicate returns `true` for the active child, and invokes that entry's `content` lambda.

## 8. `@ChildPresenter`

Use `@ChildPresenter` on a presenter class owned by a parent host presenter rather than navigated to through a route. The annotation generates a `<Presenter>ChildGraph`
graph extension exposing the presenter as a property plus a nested factory contributing to the parent's scope. The pattern fits tab pagers (Tv Maniac's progress tab
hosts an Up Next page and a Calendar page) and any other host presenter that constructs sibling presenters with `Decompose.childContext(key)`.

### Input

```kotlin
package com.thomaskioko.tvmaniac.presentation.upnext

@Inject
@ChildPresenter(
    scope = ProgressChildScope::class,
    parentScope = ProgressRoot::class,
)
public class UpNextPresenter(
    componentContext: ComponentContext,
    // ... deps
) : ComponentContext by componentContext
```

### Generated: `UpNextChildGraph.kt`

```kotlin
package com.thomaskioko.tvmaniac.presentation.upnext.di

import com.arkivanov.decompose.ComponentContext
import com.thomaskioko.tvmaniac.presentation.upnext.UpNextPresenter
import com.thomaskioko.tvmaniac.progress.nav.ProgressChildScope
import com.thomaskioko.tvmaniac.progress.nav.ProgressRoot
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides

@GraphExtension(ProgressChildScope::class)
public interface UpNextChildGraph {
    public val upNextPresenter: UpNextPresenter

    @ContributesTo(ProgressRoot::class)
    @GraphExtension.Factory
    public interface Factory {
        public fun createUpNextGraph(@Provides componentContext: ComponentContext): UpNextChildGraph
    }
}
```

### Parent presenter wiring

The parent host (here `ProgressPresenter`) takes one factory parameter per child. Each factory call gets a `Decompose.childContext(key)` so the children stay alive
together with independent lifecycles:

```kotlin
@Inject
@NavDestination(
    route = ProgressRoot::class,
    parentScope = ActivityScope::class,
    kind = DestinationKind.TAB_ROOT,
)
public class ProgressPresenter(
    componentContext: ComponentContext,
    upNextGraphFactory: UpNextChildGraph.Factory,
    calendarGraphFactory: CalendarChildGraph.Factory,
) : ComponentContext by componentContext {
    public val upNextPresenter: UpNextPresenter =
        upNextGraphFactory.createUpNextGraph(childContext(key = "UpNext")).upNextPresenter
    public val calendarPresenter: CalendarPresenter =
        calendarGraphFactory.createCalendarGraph(childContext(key = "Calendar")).calendarPresenter
}
```

The factory function name is unique per child (`create<BaseName>Graph`) so two child graphs contributing to the same parent scope (`ProgressRoot::class` here) do not
collide.

## 9. `@AppRoot`

Use `@AppRoot` on the application's `@AssistedInject` root presenter implementation. The annotation generates the activity-scope `@BindingContainer` that wires the
nested `@AssistedFactory` to the bound presenter interface. The output replaces the hand-written binding container the consumer would otherwise have to keep in sync
with the factory function name and the bound interface name.

`@AppRoot` differs from `@NavDestination` in two ways. The root has no route, so the annotation does not take a `route` parameter. The root is bound to its public
interface at the parent scope rather than exposed through a `@GraphExtension`, so the generated artifact is a binding container, not a graph plus a destination binding.

### Input

```kotlin
package com.thomaskioko.tvmaniac.presenter.root

@AppRoot(parentScope = ActivityScope::class)
@AssistedInject
public class DefaultRootPresenter(
    @Assisted componentContext: ComponentContext,
    // ... deps
) : RootPresenter, ComponentContext by componentContext {

    @AssistedFactory
    public fun interface Factory {
        public fun create(componentContext: ComponentContext): DefaultRootPresenter
    }
}
```

Where `RootPresenter` is the bound interface declared in the same module:

```kotlin
public interface RootPresenter {
    // ... presenter contract
}
```

### Generated: `RootPresenterBindingContainer.kt`

```kotlin
package com.thomaskioko.tvmaniac.presenter.root.di

@BindingContainer
@ContributesTo(ActivityScope::class)
public object RootPresenterBindingContainer {
    @Provides
    @SingleIn(ActivityScope::class)
    public fun provideRootPresenter(componentContext: ComponentContext, factory: DefaultRootPresenter.Factory): RootPresenter = factory.create(componentContext)
}
```

The `object` name is derived from the bound interface (`RootPresenter` becomes `RootPresenterBindingContainer`). The `@Provides` function name follows the same pattern
(`provideRootPresenter`). The bound interface is inferred from the implementation's supertypes; `ComponentContext`, used as a delegate, is filtered out.

## 10. `@AppRootUi`

Use `@AppRootUi` on the host composable that wraps every other screen. The annotation generates a provider interface declaring one property for each non-modifier
parameter on the composable plus a `@Composable AppRootProvider.AppRootContent(modifier)` extension that invokes the composable using the receiver's properties. The
activity-scope graph extends the generated provider, and the activity invokes `graph.AppRootContent()` instead of forwarding each dependency by hand.

The host composable is not a member of the `Set<ScreenContent>` multibinding the navigation system iterates. It is the host that publishes that set to its descendants.
`@ScreenUi` does not apply for that reason. `@AppRootUi` exists so the codegen can emit a provider interface keyed off the composable's parameter list.

### Input

```kotlin
package com.thomaskioko.tvmaniac.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thomaskioko.tvmaniac.core.base.ActivityScope
import com.thomaskioko.tvmaniac.navigation.ui.ScreenContent
import com.thomaskioko.tvmaniac.navigation.ui.SheetContent
import com.thomaskioko.tvmaniac.presenter.root.RootPresenter
import io.github.thomaskioko.codegen.annotations.AppRootUi

@AppRootUi(presenter = RootPresenter::class, parentScope = ActivityScope::class)
@Composable
public fun RootScreen(
    rootPresenter: RootPresenter,
    screenContents: Set<ScreenContent>,
    sheetContents: Set<SheetContent>,
    modifier: Modifier = Modifier,
) {
    // ... compose UI here, including the navigation host
}
```

### Generated: `RootScreenAppRootUiBinding.kt`

```kotlin
package com.thomaskioko.tvmaniac.app.ui.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thomaskioko.tvmaniac.app.ui.RootScreen
import com.thomaskioko.tvmaniac.navigation.ui.ScreenContent
import com.thomaskioko.tvmaniac.navigation.ui.SheetContent
import com.thomaskioko.tvmaniac.presenter.root.RootPresenter
import kotlin.collections.Set

public interface AppRootProvider {
    public val rootPresenter: RootPresenter

    public val screenContents: Set<ScreenContent>

    public val sheetContents: Set<SheetContent>
}

@Composable
public fun AppRootProvider.AppRootContent(modifier: Modifier = Modifier) {
    RootScreen(
        rootPresenter = rootPresenter,
        screenContents = screenContents,
        sheetContents = sheetContents,
        modifier = modifier,
    )
}
```

### Consumer wiring

The consumer makes its activity-scope `@DependencyGraph` extend `AppRootProvider`. The graph already exposes the three properties; the only change is making the
contract explicit:

```kotlin
@DependencyGraph(ActivityScope::class)
public interface ActivityGraph : AppRootProvider {
    override val rootPresenter: RootPresenter
    override val screenContents: Set<ScreenContent>
    override val sheetContents: Set<SheetContent>
    // ... other graph members
}
```

The activity then invokes the host with one call:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = ActivityGraph.create(this)
        setContent {
            graph.AppRootContent()
        }
    }
}
```
