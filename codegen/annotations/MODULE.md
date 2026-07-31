# Module annotations

Adding a screen to a navigation graph by hand means writing a graph extension for the presenter,
registering a destination factory, registering a route binding so the screen survives save and
restore, and repeating all of it for the next screen. The work is mechanical and it is easy to get
one of the four wrong.

These annotations replace that. Mark the presenter, and the processor in `codegen-processor`
generates the graph and the bindings next to it, in files the IDE can navigate to.

```kotlin
@Inject
@NavDestination(
  route = ShowsRoute::class,
  parentScope = ActivityScope::class,
  kind = DestinationKind.SCREEN,
)
class ShowsPresenter(
  componentContext: ComponentContext,
) : ComponentContext by componentContext
```

Published as `io.github.thomaskioko.gradle.plugins:codegen-annotations`.

# Package io.github.thomaskioko.codegen.annotations

The annotation set, split by what the annotated declaration is.

- `@NavDestination` marks a presenter the navigator can route to, as a screen, an overlay or a tab
- `@ChildPresenter` marks a presenter that needs a graph but is built by its parent rather than
  routed to
- `@AppRoot` marks the root of the navigation tree
- `@ScreenUi`, `@SheetUi`, `@TabUi` and `@AppRootUi` mark the composables that render them

Each annotation's own documentation lists the files it generates and the errors the processor
reports for it.
