# Get started

The navigation codegen is a KSP processor that eliminates the Metro dependency injection
boilerplate associated with Decompose-based navigation. It covers screen destinations, modal
overlays presented through Decompose's slot mechanism, tab roots, and the Android renderer
bindings that join composables to the navigation host.

A single annotation on the consumer's presenter or composable replaces a manually written Metro
`@GraphExtension`, navigation binding, or `ScreenContent` / `SheetContent` multibinding
contribution.

## Why it exists

### Why the presenter needs an annotation

Each destination in a Metro plus Decompose Kotlin Multiplatform app needs three presenter side artifacts that you would otherwise write by hand:

1. `FooRoute` (or `FooRoot`) in `nav/api`. The feature's public API. Stays manual.
2. `FooScreenGraph` (or `FooTabGraph`) in `presenter/di`. A Metro `@GraphExtension` scoped to the route, that exposes the presenter (or its assisted factory) to the activity graph.
3. `FooNavDestinationBinding` in `presenter/di`. Contributes a `NavDestination<*>` factory plus a `NavRouteBinding` or `NavRootBinding` serializer entry to the activity
   scope multibindings, so the navigator can find the destination by route type and Decompose can save and restore the back stack across process death.

The graph and binding are mechanical and derive entirely from the presenter class and the route class. `@NavDestination` generates them from one annotation. The route
stays manual because it is the feature's public API; it also doubles as the `@GraphExtension` scope marker.

### Why the composable needs an annotation

The navigation host renders whatever the navigator pushes. At runtime it holds an active `RootChild` (or `SheetChild` for overlays) whose concrete presenter type is
opaque to the host: the host is a single Compose tree at the activity root, but the active child can be any feature's presenter. To render the matching Android
composable, the host needs a registry that maps a presenter type to a composable.

That registry is the `Set<ScreenContent>` (and `Set<SheetContent>`) multibinding. Each entry pairs a `matches` predicate (does this entry handle the active child?) with a
`content` lambda (here is how to render it). The host iterates the set, picks the entry whose predicate returns `true`, and invokes its `content` lambda.

Without `@ScreenUi` or `@SheetUi`, every feature has to write a `ScreenContent` (or `SheetContent`) binding by hand: a `@Provides @IntoSet` function that constructs the
right predicate (`(it as? ScreenDestination<*>)?.presenter is FooPresenter`) and the right content lambda (cast the child, cast its presenter, invoke the composable).
That binding is mechanical and derives entirely from the composable function reference and the presenter type, so `@ScreenUi` and `@SheetUi` generate it instead. The
composable itself stays manual because it is the feature's UI; the annotation marks the entry point the codegen reads to produce the registry entry.

For how the processor turns each annotation into Metro plus Decompose code, see [architecture/index.md](architecture/index.md).

## Supported annotations

Seven annotations cover every variant the processor knows how to generate. See [annotations.md](annotations.md) for the full reference and [examples.md](examples.md) for
concrete inputs and outputs.

The shared code presenter annotations (target `CLASS`, live in the Kotlin Multiplatform `presenter` module):

1. `@NavDestination(route, parentScope, kind)` is one annotation for every navigation destination. `kind` is one of `DestinationKind.SCREEN`, `OVERLAY`, or `TAB_ROOT`.
   - `SCREEN` and `OVERLAY` generate a graph scoped to the route plus a binding that contributes `NavDestination.Screen` (or `Overlay`) and `NavRouteBinding`. The
     processor auto-detects `@AssistedInject` with a nested `@AssistedFactory` to switch between the two presenter forms (one with no runtime parameters, one
     parameterized).
   - `TAB_ROOT` generates a graph scoped to the root plus a binding that contributes `NavDestination.TabRoot`, `NavRootBinding`, and the route singleton into `Set<NavRoot>`. Plain `@Inject` only.
2. `@AppRoot(parentScope)` marks the application's `@AssistedInject` root presenter implementation. The processor generates the `@BindingContainer` that wires the
   nested `@AssistedFactory` to the bound presenter interface at the parent scope. The root is bound directly into the scope rather than exposed through a graph
   extension because the activity holds it for the lifetime of the scope.
3. `@ChildPresenter(scope, parentScope)` marks a presenter constructed by another presenter rather than navigated to through a route, such as a tab pager's pages. The
   processor generates a `<Presenter>ChildGraph` graph extension exposing the presenter as a property plus a factory contributing to the parent host's scope. The parent
   host takes one factory per child and instantiates each child with a `Decompose.childContext(key)`.

The Android UI renderer annotations (target `FUNCTION`, live in the Android `ui` module):

4. `@ScreenUi` marks a `@Composable` function as the Android renderer for a screen presenter defined in the shared Kotlin Multiplatform layer. It generates a
   `@BindingContainer` object that contributes a `ScreenContent` into `Set<ScreenContent>` so the navigation host can iterate the set and render the right screen.
5. `@SheetUi` marks a `@Composable` function as the Android renderer for a modal overlay presenter. It contributes a `SheetContent` into `Set<SheetContent>`.
6. `@TabUi` marks a `@Composable` function as the Android renderer for one tab pager page. The generated binding is identical in shape to a `@ScreenUi` binding except
   that the predicate matches `TabChild<*>` rather than `ScreenDestination<*>`. Use it on the four bottom-bar tab pages where the active child is a `TabChild`-wrapped tab
   presenter rather than a `ScreenDestination`-wrapped routed screen.
7. `@AppRootUi(presenter, parentScope)` marks the host composable that wraps every other screen. The processor reads the function's non-modifier parameters and emits an
   `AppRootProvider` interface plus a `@Composable AppRootProvider.AppRootContent(modifier)` extension. The activity-scope graph extends `AppRootProvider`, and the
   activity invokes `graph.AppRootContent()` instead of forwarding each dependency by hand.

## Dependency

1. Apply the plugin DSL. In a Kotlin Multiplatform presenter module's `build.gradle.kts`:

   ```kotlin
   plugins {
       alias(libs.plugins.app.kmp)
   }

   scaffold {
       useCodegen()
   }
   ```


   `useCodegen()` is also the entry point in an Android `ui` module that uses `@ScreenUi` or `@SheetUi`. The Android module typically pairs it with `useCompose()` inside
   the `android` block:


   ```kotlin
   plugins {
       alias(libs.plugins.app.android)
   }

   scaffold {
       useCodegen()

       android {
           useCompose()
       }
   }
   ```

   `useCodegen()` applies the KSP plugin, adds `codegen-annotations` to the appropriate implementation configuration, and registers `codegen-processor` as a KSP processor
   for every target in the module.

2. Declare the two library entries in the consumer's `libs.versions.toml` so the DSL can resolve them through the version catalog:

   ```toml
   [libraries]
   codegen-annotations = { module = "io.github.thomaskioko.gradle.plugins:codegen-annotations", version.ref = "app-gradle-plugins" }
   codegen-processor = { module = "io.github.thomaskioko.gradle.plugins:codegen-processor", version.ref = "app-gradle-plugins" }
   ```

## Basic usage

Annotate the presenter (shared Kotlin Multiplatform layer):

```kotlin
@Inject
@NavDestination(
    route = DebugRoute::class,
    parentScope = ActivityScope::class,
    kind = DestinationKind.SCREEN,
)
public class DebugPresenter(...) : ComponentContext by componentContext
```

Annotate the matching composable (Android `ui` layer):

```kotlin
@ScreenUi(
    presenter = DebugPresenter::class,
    parentScope = ActivityScope::class
)
@Composable
public fun DebugMenuScreen(
    presenter: DebugPresenter,
    modifier: Modifier = Modifier,
) { ... }
```

For the application's root, annotate the root presenter implementation and the host composable (one pair per project):

```kotlin
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

@AppRootUi(presenter = RootPresenter::class, parentScope = ActivityScope::class)
@Composable
public fun RootScreen(
    rootPresenter: RootPresenter,
    screenContents: Set<ScreenContent>,
    sheetContents: Set<SheetContent>,
    modifier: Modifier = Modifier,
) { ... }
```

Make the activity-scope graph extend the generated `AppRootProvider` so the generated extension resolves at the call site:

```kotlin
@DependencyGraph(ActivityScope::class)
public interface ActivityGraph : AppRootProvider {
    override val rootPresenter: RootPresenter
    override val screenContents: Set<ScreenContent>
    override val sheetContents: Set<SheetContent>
    // ...
}
```

The activity then invokes `graph.AppRootContent()` instead of forwarding each parameter to `RootScreen` by hand.

Build the modules. KSP generates the graph and navigation binding into the presenter module's `di/` package, the `ScreenContent` binding into the `ui` module's `di/`
package, and the root binding container plus the `AppRootProvider` interface into their respective modules. No further wiring is required inside each module.

The app module must declare a direct `implementation` dependency on each feature `ui` module, not a transitive one. A transitive `implementation` dependency through a
root `ui` module does not put the generated bindings on the app's compile classpath. Metro then reports a build error because the `Set<ScreenContent>` (or
`Set<SheetContent>`) multibinding is empty.

## Common questions

**Can I reuse one route across multiple presenters?**
No. Each presenter has its own unique route class. The route class doubles as the graph extension's scope marker, so reusing it across presenters would mean two graphs
sharing one scope, which Metro rejects. If you want two presenters to share a value, model it as a parameter on the route and let each route be its own type.

**Can my parameterized presenter take more than one runtime parameter?**
Not today. The processor expects exactly one `@Assisted` constructor parameter on a parameterized presenter and one matching property on the route class. If you need
more, fold the inputs into a single value type and pass that as the assisted parameter.

For example, an episode details screen that needs both a show ID and a season number does not declare two assisted parameters:

```kotlin
// Won't work. The processor reports a compile error because the presenter
// has two @Assisted parameters.
@AssistedInject
@NavDestination(route = EpisodeRoute::class, parentScope = ActivityScope::class, kind = SCREEN)
public class EpisodePresenter(
    @Assisted private val showId: Long,
    @Assisted private val seasonNumber: Int,
    componentContext: ComponentContext,
)

@Serializable
public data class EpisodeRoute(
    public val showId: Long,
    public val seasonNumber: Int,
) : NavRoute
```

Wrap the two values in a single param type and assist on the wrapper:

```kotlin
@Serializable
public data class EpisodeParam(
    public val showId: Long,
    public val seasonNumber: Int,
)

@AssistedInject
@NavDestination(route = EpisodeRoute::class, parentScope = ActivityScope::class, kind = SCREEN)
public class EpisodePresenter(
    @Assisted private val param: EpisodeParam,
    componentContext: ComponentContext,
) {
    @AssistedFactory
    public fun interface Factory {
        public fun create(param: EpisodeParam): EpisodePresenter
    }
}

@Serializable
public data class EpisodeRoute(public val param: EpisodeParam) : NavRoute
```

The route now has one property (`param`) whose type matches the presenter's one `@Assisted` parameter. Adding a multi parameter form to the codegen itself would be a
generator change in `NavDestinationBindingGenerator.destinationBody`.

**Where does state save and restore happen?**
Each generated binding contributes a `NavRouteBinding` or `NavRootBinding` entry that pairs the route class with its `KSerializer`. The consumer's serialization layer
iterates that multibinding to build a `SerializersModule` that Decompose uses to encode the back stack on process death and decode it on relaunch. The codegen never
serialises anything itself; it only contributes the entries. See [architecture/consumer-contract.md](architecture/consumer-contract.md#state-save-and-restore).

## References

- Decompose: https://arkivanov.github.io/Decompose/
- Metro: https://zacsweers.github.io/metro/
- KSP: https://kotlinlang.org/docs/ksp-overview.html
- kctfork: https://github.com/ZacSweers/kotlin-compile-testing
- KotlinPoet: https://square.github.io/kotlinpoet/
