# Get started

The navigation codegen is a KSP processor that writes the Metro dependency injection code for Decompose navigation. It covers screens, modal overlays (shown through Decompose's slot), tab roots, and the Android renderer bindings that connect composables to the navigation host.

You put one annotation on a presenter or a composable. It replaces a Metro `@GraphExtension`, a navigation binding, or a `ScreenContent` / `SheetContent` multibinding contribution that you would otherwise write by hand.

## Why it exists

### Why the presenter needs an annotation

In a Kotlin Multiplatform app built on Metro and Decompose, every destination needs three files on the presenter's side:

1. `FooRoute` (or `FooRoot`) in `nav/api`. The feature's public API. Stays manual.
2. `FooScreenGraph` (or `FooTabGraph`) in `presenter/di`. A Metro `@GraphExtension` scoped to the route, that exposes the presenter (or its assisted factory) to the activity graph.
3. `FooNavDestinationBinding` in `presenter/di`. Contributes a `NavDestination<*>` factory plus a `NavRouteBinding` or `NavRootBinding` serializer entry to the activity
   scope multibindings, so the navigator can find the destination by route type and Decompose can save and restore the back stack across process death.

You write the route. The graph and the binding are mechanical. Everything in them comes from the presenter class and the route class. So `@NavDestination` generates both. We keep the route manual because it is the feature's public API. It also doubles as the scope marker for the `@GraphExtension`.

### Why the composable needs an annotation

The navigation host renders whatever the navigator pushes. It is a single Compose tree at the activity root. At runtime it holds an active `RootChild` (or `SheetChild` for overlays), and the presenter inside can belong to any feature. The host doesn't know its concrete type. So it needs a registry that maps a presenter type to the composable that renders it.

That registry is the `Set<ScreenContent>` (and `Set<SheetContent>`) multibinding. Each entry has two parts. A `matches` predicate answers "do I handle the active child?". A `content` lambda renders it. The host walks the set, picks the entry whose predicate returns `true`, and calls its `content` lambda.

Without `@ScreenUi` or `@SheetUi`, every feature writes that entry by hand. It is a `@Provides @IntoSet` function with the right predicate (`(it as? ScreenDestination<*>)?.presenter is FooPresenter`) and the right content lambda (cast the child, cast its presenter, call the composable). Everything in it comes from the composable function and the presenter type, so `@ScreenUi` and `@SheetUi` generate it. The composable stays manual, since it is the feature's UI. The annotation just marks the entry point the codegen reads.

For how the processor turns each annotation into Metro plus Decompose code, see [architecture/index.md](../internals/index.md).

## Supported annotations

The processor supports seven annotations. See [annotations.md](annotations.md) for the full reference and [examples.md](examples.md) for
concrete inputs and outputs.

Presenter annotations (target `CLASS`, used in the shared Kotlin Multiplatform `presenter` module):

1. `@NavDestination(route, parentScope, kind)` is one annotation for every navigation destination. `kind` is one of `DestinationKind.SCREEN`, `OVERLAY`, or `TAB_ROOT`.
   - `SCREEN` and `OVERLAY` generate a graph scoped to the route plus a binding that contributes `NavDestination.Screen` (or `Overlay`) and `NavRouteBinding`. The
     processor auto-detects `@AssistedInject` with a nested `@AssistedFactory` to switch between the two presenter forms (one with no runtime parameters, one
     parameterized).
   - `TAB_ROOT` generates a graph scoped to the root plus a binding that contributes `NavDestination.TabRoot`, `NavRootBinding`, and the route singleton into `Set<NavRoot>`. Plain `@Inject` only.
2. `@AppRoot(parentScope)` marks the application's `@AssistedInject` root presenter implementation. The processor generates the `@BindingContainer` that wires the
   nested `@AssistedFactory` to the bound presenter interface at the parent scope. We bind the root directly into the scope instead of exposing it through a graph
   extension, because the activity holds it for the whole lifetime of the scope.
3. `@ChildPresenter(scope, parentScope)` marks a presenter constructed by another presenter rather than navigated to through a route, such as a tab pager's pages. The
   processor generates a `<Presenter>ChildGraph` graph extension exposing the presenter as a property plus a factory contributing to the parent host's scope. The parent
   host takes one factory per child and instantiates each child with a `Decompose.childContext(key)`.

Android renderer annotations (target `FUNCTION`, used in the Android `ui` module):

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
package, and the root binding container plus the `AppRootProvider` interface into their respective modules. You don't need any other wiring inside each module.

The app module needs a direct `implementation` dependency on each feature `ui` module. A transitive one won't do. If the dependency only comes through a root `ui`
module, the generated bindings are not on the app's compile classpath. Metro then fails the build because the `Set<ScreenContent>` (or `Set<SheetContent>`)
multibinding is empty.

## Common questions

**Can I reuse one route across multiple presenters?**
No. Each presenter has its own route class. The route class is also the graph extension's scope marker. Reusing it would give two graphs one scope, and Metro rejects
that. If two presenters need to share a value, make it a parameter on the route and give each presenter its own route type.

**Can my parameterized presenter take more than one runtime parameter?**
Not today. A parameterized presenter must have exactly one `@Assisted` constructor parameter, and the route class one matching property. If you need more, fold the
inputs into a single value type and pass that as the assisted parameter.

Say an episode details screen needs both a show ID and a season number. Don't declare two assisted parameters:

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

The route now has one property (`param`), and its type matches the presenter's one `@Assisted` parameter. Supporting several parameters in the codegen itself would
mean changing the generator in `NavDestinationBindingGenerator.destinationBody`.

**Where does state save and restore happen?**
Each generated binding contributes a `NavRouteBinding` or `NavRootBinding` entry that pairs the route class with its `KSerializer`. Your serialization layer walks that
multibinding and builds a `SerializersModule`. Decompose uses it to encode the back stack on process death and decode it on relaunch. The codegen never serialises
anything itself. It only contributes the entries. See [architecture/consumer-contract.md](../internals/consumer-contract.md#state-save-and-restore).

## References

- Decompose: https://arkivanov.github.io/Decompose/
- Metro: https://zacsweers.github.io/metro/
- KSP: https://kotlinlang.org/docs/ksp-overview.html
- kctfork: https://github.com/ZacSweers/kotlin-compile-testing
- KotlinPoet: https://square.github.io/kotlinpoet/
