# Examples

Concrete input and generated output for every annotation shape. Each example is taken from the golden fixtures used by `codegen-processor-test`.

## Shape 1: Simple `@NavScreen` (plain `@Inject`)

Use this shape for a root destination whose presenter needs no runtime parameters. Everything it depends on is provided by Metro. The processor sees a plain `@Inject` constructor and generates a graph that exposes the presenter instance directly, plus a `NavDestination` that instantiates it with just a `ComponentContext`.

### Input

```kotlin
package com.thomaskioko.tvmaniac.debug.presenter

@Inject
@NavScreen(route = DebugRoute::class, parentScope = ActivityScope::class)
public class DebugPresenter(
    componentContext: ComponentContext,
    private val navigator: DebugNavigator,
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
        public fun provideDebugNavDestination(graphFactory: DebugScreenGraph.Factory): NavDestination = object : NavDestination {
            override fun matches(route: NavRoute): Boolean = route is DebugRoute

            override fun createChild(route: NavRoute, componentContext: ComponentContext): RootChild =
                ScreenDestination(presenter = graphFactory.createDebugGraph(componentContext).debugPresenter)
        }

        @Provides
        @IntoSet
        public fun provideDebugRouteBinding(): NavRouteBinding<*> =
            NavRouteBinding(DebugRoute::class, DebugRoute.serializer())
    }
}
```

## Shape 2: Parameterized `@NavScreen` (`@AssistedInject`)

Use this shape when the presenter needs a value carried on the route itself, such as a show id or an episode id that identifies the specific instance of the screen. The processor detects `@AssistedInject` with a nested `@AssistedFactory`, exposes the factory on the graph, and generates a `NavDestination.createChild` that casts the incoming route and threads its property through `factory.create(...)`.

### Input

```kotlin
package com.thomaskioko.tvmaniac.presenter.showdetails

@AssistedInject
@NavScreen(route = ShowDetailsRoute::class, parentScope = ActivityScope::class)
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
        ): NavDestination = object : NavDestination {
            override fun matches(route: NavRoute): Boolean = route is ShowDetailsRoute

            override fun createChild(route: NavRoute, componentContext: ComponentContext): RootChild {
                val showRoute = route as ShowDetailsRoute
                val graph = graphFactory.createShowDetailsGraph(componentContext)
                return ScreenDestination(graph.showDetailsFactory.create(showRoute.param))
            }
        }

        @Provides
        @IntoSet
        public fun provideShowDetailsRouteBinding(): NavRouteBinding<*> =
            NavRouteBinding(ShowDetailsRoute::class, ShowDetailsRoute.serializer())
    }
}
```

### Route / factory shape invariant

The processor expects:

- Exactly one `@Assisted` constructor parameter on the presenter.
- Exactly one property on the route class whose type matches the assisted parameter's type.

If either invariant is violated, the processor emits a compile error pointing at the offending declaration.

## Shape 3: `@TabScreen`

Use this shape for a tab inside a host screen (e.g. the home screen's bottom tabs). Tabs don't participate in the root navigation stack, so they contribute a `TabDestination` into the host's multibinding instead of a `NavDestination`. No `NavRouteBinding` is emitted because the host feature owns its own config serialization.

### Input

```kotlin
package com.thomaskioko.tvmaniac.discover.presenter

@Inject
@TabScreen(config = HomeConfig.Discover::class, parentScope = HomeRoute::class)
public class DiscoverShowsPresenter(
    componentContext: ComponentContext,
    // ... deps
) : ComponentContext by componentContext
```

The generated tab graph is `@GraphExtension(HomeConfig.Discover::class)`. The config class itself is the scope marker. The factory is `@ContributesTo(HomeRoute::class)` so that every tab joins the host home graph whose scope is `HomeRoute`.

### Generated: `DiscoverShowsTabDestinationBinding.kt`

```kotlin
@ContributesTo(HomeRoute::class)
public interface DiscoverShowsTabDestinationBinding {
    public companion object {
        @Provides
        @IntoSet
        public fun provideDiscoverShowsTabDestination(
            graphFactory: DiscoverShowsTabGraph.Factory,
        ): TabDestination = object : TabDestination {
            override fun matches(config: HomeConfig): Boolean = config is HomeConfig.Discover

            override fun createChild(config: HomeConfig, componentContext: ComponentContext): TabChild<*> =
                TabChild(graphFactory.createDiscoverShowsTabGraph(componentContext).discoverShowsPresenter)
        }
    }
}
```

Notice: no `NavRouteBinding` provider. Home owns its own `HomeConfig` serialization so the route binding multibinding is not used for tabs.

## Shape 4: `@NavSheet`

Use this shape for a modal sheet — bottom sheet, dialog, or overlay — presented on top of the current destination via Decompose's `childSlot`. The sheet has its own `SheetConfig` carrying any assisted parameters, and requires `@AssistedInject` so those parameters can flow from the config into the presenter. The generated binding mirrors `@NavScreen` but targets the sheet multibindings (`SheetChildFactory` + `SheetConfigBinding`) so the slot survives process death without a central registry.

### Input

```kotlin
package com.thomaskioko.tvmaniac.presenter.episodedetail

@AssistedInject
@NavSheet(
    route = EpisodeSheetConfig::class,
    parentScope = ActivityScope::class,
)
public class EpisodeDetailSheetPresenter(
    @Assisted public val episodeId: Long,
    @Assisted public val source: ScreenSource,
    componentContext: ComponentContext,
    // ... deps
) {
    @AssistedFactory
    public fun interface Factory {
        public fun create(episodeId: Long, source: ScreenSource): EpisodeDetailSheetPresenter
    }
}
```

`EpisodeSheetConfig` is a `@Serializable` data class in the feature's `nav/api` module that implements the consumer's `SheetConfig` marker. The generated sheet graph is `@GraphExtension(EpisodeSheetConfig::class)`. The config class itself is the scope marker.

### Generated: `EpisodeDetailSheetDestinationBinding.kt`

```kotlin
@ContributesTo(ActivityScope::class)
public interface EpisodeDetailSheetDestinationBinding {
    public companion object {
        @Provides
        @IntoSet
        public fun provideEpisodeDetailSheetChildFactory(
            graphFactory: EpisodeDetailSheetScreenGraph.Factory,
        ): SheetChildFactory = object : SheetChildFactory {
            override fun matches(config: SheetConfig): Boolean = config is EpisodeSheetConfig

            override fun createChild(
                config: SheetConfig,
                componentContext: ComponentContext,
            ): SheetChild {
                val sheetConfig = config as EpisodeSheetConfig
                return SheetDestination(
                    presenter = graphFactory.createEpisodeDetailSheetGraph(componentContext)
                        .episodeDetailSheetFactory.create(sheetConfig.episodeId, sheetConfig.source),
                )
            }
        }

        @Provides
        @IntoSet
        public fun provideEpisodeDetailSheetConfigBinding(): SheetConfigBinding<*> =
            SheetConfigBinding(EpisodeSheetConfig::class, EpisodeSheetConfig.serializer())
    }
}
```

Notice the shape is parallel to `@NavScreen`: a `@ContributesTo(parentScope)` interface whose companion contributes two `@IntoSet` providers. `SheetChildFactory` feeds the `Set<SheetChildFactory>` multibinding that the root presenter walks when the sheet slot activates. `SheetConfigBinding<*>` feeds the polymorphic `KSerializer<SheetConfig>` used by Decompose's `childSlot`, so the sheet slot survives process death without any central registry.

The processor supports multiple `@Assisted` parameters for sheets. It maps each assisted constructor parameter to a route property by type and order.
