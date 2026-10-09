# Examples

This page shows the input and generated output for every annotation variant. Each example comes from
the golden fixtures used by `codegen-processor-test`, so the output matches what the processor
produces in a real build.

Each section shows the annotated declaration you write, then every Kotlin file the processor writes
for it. Generated files land in a `<package>.di` sub-package next to the annotated symbol. For how
each file is built from the input, see [architecture/generators.md](../internals/generators.md) and
[architecture/parsers.md](../internals/parsers.md).

## Contents

1. [`@NavDestination(kind = SCREEN)`, presenter with no runtime parameters](#1-navdestinationkind-screen-presenter-with-no-runtime-parameters)
2. [`@NavDestination(kind = SCREEN)`, parameterized presenter](#2-navdestinationkind-screen-parameterized-presenter)
3. [`@NavDestination(kind = OVERLAY)`](#3-navdestinationkind-overlay)
4. [`@NavDestination(kind = TAB_ROOT)`](#4-navdestinationkind-tab_root)
5. [`@ScreenUi`](#5-screenui)
6. [`@SheetUi`](#6-sheetui)
7. [`@TabUi`](#7-tabui)
8. [`@ChildPresenter`](#8-childpresenter)
9. [`@AppRoot`](#9-approot)
10. [`@AppRootUi`](#10-approotui)

## 1. `@NavDestination(kind = SCREEN)`, presenter with no runtime parameters

Use `@NavDestination(kind = SCREEN)` on a presenter with a plain `@Inject` constructor, when it needs no runtime parameters from the route. Metro provides every
dependency from the surrounding dependency graph. `@NavDestination` generates a graph that exposes the presenter instance directly. It also generates a
`NavDestination.Screen` factory that builds the presenter from a `ComponentContext` alone.

This is the simpler of the two SCREEN forms. The next section covers presenters that take one runtime parameter through `@AssistedInject`.

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

Use `@NavDestination(kind = SCREEN)` on a presenter with `@AssistedInject` and a nested `@AssistedFactory`, when it needs one runtime value from the route. Think of a
show ID or an episode ID that identifies which screen instance this is. The value lives as a property on the route class.

The difference from the previous section is the constructor. It has `@AssistedInject`, one `@Assisted` argument, and a nested `@AssistedFactory` interface.
`@NavDestination` detects the assisted factory and exposes it on the generated graph instead of the presenter. The generated factory lambda reads the matching property
from the incoming route and passes it to `factory.create(...)`.

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

Break either rule and the processor reports a compile error on the offending declaration. The rules are what let the processor match the route property to the
assisted factory parameter.

- The presenter must have exactly one `@Assisted` constructor parameter.
- The route class must have exactly one property whose type matches the assisted parameter's type.

## 3. `@NavDestination(kind = OVERLAY)`

Use `@NavDestination(kind = OVERLAY)` for a modal destination shown on top of the current screen through Decompose's slot, such as a bottom sheet, dialog, or menu.
Two things differ from a SCREEN destination. First, the route implements `NavRoute` plus a marker interface (in Tv Maniac, `OverlayRoute`). The marker tells the
navigator to put the destination in the overlay slot instead of pushing it onto the back stack. Second, the generated binding contributes a `NavDestination.Overlay`
instead of a `NavDestination.Screen`.

`@NavDestination(kind = OVERLAY)` works with both plain `@Inject` and `@AssistedInject` presenters. The example below uses the parameterized form. The full runtime flow
is in [architecture/consumer-contract.md](../internals/consumer-contract.md#runtime-flow).

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

The graph file has the same form as `ShowDetailsScreenGraph` in example 2. The only difference between SCREEN and OVERLAY output is the binding: it contributes a
`NavDestination.Overlay` instead of a `NavDestination.Screen`. The navigator checks that subclass at runtime to decide whether to push the destination onto the back
stack or show it in Decompose's overlay slot.

## 4. `@NavDestination(kind = TAB_ROOT)`

Use `@NavDestination(kind = TAB_ROOT)` for the destination shown when the user selects a bottom navigation tab. What differs from SCREEN and OVERLAY is the route
type. A tab root's route is a `NavRoot` `data object`, not a `NavRoute` `data class`, so it carries no payload. That is why tab presenters use plain `@Inject` only.
`@NavDestination` reports a compile error if a tab presenter declares a nested `@AssistedFactory`.

The generated binding contributes a `NavDestination.TabRoot` (instead of `Screen` or `Overlay`) plus a `NavRootBinding<*>` (instead of `NavRouteBinding<*>`). That lets
the tab root take part in polymorphic save and restore alongside the other tabs. It also contributes the route singleton itself into `Set<NavRoot>`. That replaces the
`<Feature>RootBinding` files consumers used to write by hand next to each tab.

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

The tab graph is contributed to `parentScope` (usually `ActivityScope`), the same scope as the unified `Set<NavDestination<*>>`. The home presenter filters by the
`TabRoot` subclass and renders the active root. The third contribution feeds `Set<NavRoot>`. A navigator usually walks that set to list the available tabs without
looking at destination factories.

## 5. `@ScreenUi`

Use `@ScreenUi` on the Android `@Composable` function that renders a screen presenter. The annotation generates a `ScreenContent` binding that adds the composable to the
`Set<ScreenContent>` multibinding. The navigation host walks that set to pick the renderer for the active screen. Without `@ScreenUi`, you write this binding file by
hand for every composable.

The previous four sections cover the annotation on the presenter. `@ScreenUi` is the annotation on the Android UI that pairs with a `kind = SCREEN` presenter at
runtime. The next section covers the overlay equivalent, `@SheetUi`.

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

We emit a `@BindingContainer object` instead of `interface + companion object` on purpose. The Android only `ui` source set doesn't pick up `@Provides @IntoSet`
declarations from a companion object the way the shared Kotlin Multiplatform source set does. So the interface form would silently give an empty multibinding at
runtime. The full reasoning is in
[architecture/generators.md](../internals/generators.md#bindingcontainer-object-for-ui-bindings-interface-companion-for-destination-bindings).

### Composable signature

The annotated function must take exactly two parameters: `presenter: <PresenterType>` first and `modifier: Modifier = Modifier` second. The generator passes them by
name, so renaming either breaks the generated code at the next compile.

## 6. `@SheetUi`

Use `@SheetUi` on the Android `@Composable` function that renders an overlay presenter. It differs from `@ScreenUi` in the multibinding it contributes to. `@SheetUi`
adds a `SheetContent` into `Set<SheetContent>`, because the overlay slot walks the sheet set. `@ScreenUi` adds a `ScreenContent` into `Set<ScreenContent>`, because the
navigation stack walks the screen set. `@SheetUi` also doesn't forward `Modifier` to the composable, while `@ScreenUi` does. The reason is at the end of this section.

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

The overlay renderer doesn't receive a modifier. `SheetContent.content` is typed as `@Composable (SheetChild) -> Unit`. Modal layout decisions (a `ModalBottomSheet`, for
example) belong inside the composable body, not at the call site. The annotated function still takes a `modifier: Modifier = Modifier` parameter to match other
composables, but the generator doesn't forward it.

## 7. `@TabUi`

Use `@TabUi` on the Android `@Composable` function that renders one tab pager page. It generates a `ScreenContent` binding with the same shape as the `@ScreenUi`
output, but the predicate matches `TabChild<*>` instead of `ScreenDestination<*>`. Use it on the four bottom bar tab pages (Discover, Library, Progress, Profile). There
the active child is a tab presenter wrapped in a `TabChild`, not a routed screen wrapped in a `ScreenDestination`.

The previous section covered the overlay renderer (`@SheetUi`). The next section covers child presenters that a parent presenter owns (`@ChildPresenter`). The two after
that cover the application root pair (`@AppRoot` and `@AppRootUi`).

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

The output goes into the same `Set<ScreenContent>` multibinding the navigation host walks. The host treats `TabChild` and `ScreenDestination` the same way. It walks the
set, finds the entry whose predicate returns `true` for the active child, and calls that entry's `content` lambda.

## 8. `@ChildPresenter`

Use `@ChildPresenter` on a presenter that a parent host presenter owns, instead of one the navigator reaches through a route. The annotation generates a
`<Presenter>ChildGraph` graph extension that exposes the presenter as a property, plus a nested factory contributed to the parent's scope. This fits tab pagers (Tv
Maniac's progress tab hosts an Up Next page and a Calendar page). It also fits any other host presenter that builds sibling presenters with
`Decompose.childContext(key)`.

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

The parent host (here `ProgressPresenter`) takes one factory parameter per child. Each factory call gets a `Decompose.childContext(key)`, so the children stay alive
together but keep independent lifecycles:

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

Each child gets its own factory function name (`create<BaseName>Graph`). So two child graphs contributed to the same parent scope (`ProgressRoot::class` here) don't
collide.

### Reusable components

The example above ties the child to one host, because `parentScope` is `ProgressRoot::class`. A
component meant to live in its own module and be reused (say, a featured shows hero) sets
`parentScope` to a shared ancestor scope instead: `ActivityScope::class`. The generated factory then
contributes to a graph every screen descends from, so any host below `ActivityScope` can embed it.

```kotlin
package com.thomaskioko.tvmaniac.presentation.featured

@Inject
@ChildPresenter(
    scope = FeaturedShowsComponentScope::class,
    parentScope = ActivityScope::class,
)
public class FeaturedShowsPresenter(
    componentContext: ComponentContext,
    // ... deps available at ActivityScope or AppScope
) : ComponentContext by componentContext
```

The only difference in the generated graph is the factory's `@ContributesTo` target:

```kotlin
package com.thomaskioko.tvmaniac.presentation.featured.di

@GraphExtension(FeaturedShowsComponentScope::class)
public interface FeaturedShowsChildGraph {
    public val featuredShowsPresenter: FeaturedShowsPresenter

    @ContributesTo(ActivityScope::class)
    @GraphExtension.Factory
    public interface Factory {
        public fun createFeaturedShowsGraph(@Provides componentContext: ComponentContext): FeaturedShowsChildGraph
    }
}
```

A discover host and a search host both embed it with the same two lines. An embeddable component
can nest another one the same way, since the outer graph also descends from `ActivityScope`:

```kotlin
public val featuredPresenter: FeaturedShowsPresenter =
    featuredGraphFactory.createFeaturedShowsGraph(childContext(key = "Featured")).featuredShowsPresenter
```

There is one constraint. An embeddable component may inject only bindings reachable at `ActivityScope`
or `AppScope`. A dependency on a binding scoped to one tab root fails as an ordinary Metro
missing-binding error at the embedding site.

## 9. `@AppRoot`

Use `@AppRoot` on the application's `@AssistedInject` root presenter implementation. The annotation generates the `@BindingContainer` in the activity scope that
connects the nested `@AssistedFactory` to the bound presenter interface. Without it, you write that binding container by hand and keep it in sync with the factory
function name and the bound interface name.

`@AppRoot` differs from `@NavDestination` in two ways. First, the root has no route, so the annotation has no `route` parameter. Second, the root is bound to its
public interface at the parent scope instead of exposed through a `@GraphExtension`. So the generated file is a binding container, not a graph plus a destination
binding.

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

The `object` name comes from the bound interface (`RootPresenter` becomes `RootPresenterBindingContainer`). The `@Provides` function name follows the same pattern
(`provideRootPresenter`). The bound interface is inferred from the implementation's supertypes. `ComponentContext`, used as a delegate, is skipped.

## 10. `@AppRootUi`

Use `@AppRootUi` on the host composable that wraps every other screen. The annotation generates a provider interface with one property for each non-modifier parameter
of the composable. It also generates a `@Composable AppRootProvider.AppRootContent(modifier)` extension that calls the composable with the receiver's properties. The
activity-scope graph extends the generated provider, and the activity calls `graph.AppRootContent()` instead of forwarding each dependency by hand.

The host composable is not part of the `Set<ScreenContent>` multibinding the navigation system walks. It is the host that provides that set to its descendants, so
`@ScreenUi` doesn't apply. `@AppRootUi` lets the codegen emit a provider interface built from the composable's parameter list.

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

### Wiring

Make your activity-scope `@DependencyGraph` extend `AppRootProvider`. The graph already exposes the three properties. The only change is making the contract
explicit:

```kotlin
@DependencyGraph(ActivityScope::class)
public interface ActivityGraph : AppRootProvider {
    override val rootPresenter: RootPresenter
    override val screenContents: Set<ScreenContent>
    override val sheetContents: Set<SheetContent>
    // ... other graph members
}
```

The activity then renders the host with one call:

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
