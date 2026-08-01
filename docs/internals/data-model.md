# Data model

Parsers do not feed KSP types directly to KotlinPoet generators. Every parser produces a typed intermediate value and the generators consume nothing else. This
intermediate value lives in `codegen/processor/src/main/kotlin/io/github/thomaskioko/codegen/processor/data/`.

There are five top level types. Presenter side navigation annotations (`@NavDestination`) resolve to a `NavData`. UI renderer annotations (`@ScreenUi`, `@SheetUi`,
`@TabUi`) resolve to a `UiBindingData`. The application root presenter annotation (`@AppRoot`) resolves to an `AppRootData`. The application root composable annotation
(`@AppRootUi`) resolves to an `AppRootUiData`. The parent-owned child presenter annotation (`@ChildPresenter`) resolves to a `ChildPresenterData`. The five are independent
and share no supertype because the downstream generators are structurally different.

## NavData

`NavData` is a sealed interface with two implementations: `ScreenData` for stack screens and modal overlays, `TabData` for top level tab roots.

```kotlin
internal sealed interface NavData {
    val presenterClass: ClassName
    val baseName: String
    val packageName: String
    val parentScope: ClassName
    val scope: ClassName
    val graphClassName: ClassName
    val graphFactoryFunName: String
    val bindingClassName: ClassName
    val graphPropertyType: ClassName
    val graphPropertyName: String
    val graphFactoryClassName: ClassName
        get() = graphClassName.nestedClass("Factory")
}
```

`ScreenData` carries the route class, an optional `@AssistedFactory` `ClassName`, the route property name (when the presenter accepts a runtime parameter), and a
`ScreenKind` enum (`SCREEN` or `OVERLAY`) that the binding generator uses to pick `NavDestination.Screen` or `NavDestination.Overlay`. The `factory` field acts as the
parameterization marker: when `factory` is `null` the presenter uses plain `@Inject` and the generated graph exposes the presenter directly; when `factory` is non `null`
the presenter uses `@AssistedInject` and the generated graph exposes the factory. The derived `isParameterized` flag reads `factory != null`.

`TabData` carries the route plus a `configEnclosing` field for nested route classes. It has no factory branch because tabs are always plain `@Inject`. The parser rejects
`@AssistedInject` tab presenters explicitly; see [parsers.md](parsers.md).

The `graphPropertyName` and `graphPropertyType` fields are the one place where the screen branch and the tab branch produce different output downstream. `ScreenData`
resolves them to the factory's name and class when the presenter is parameterized, and to the presenter's name and class otherwise. `TabData` always resolves to the
presenter.

### Naming on the data class

The derived properties (`graphClassName`, `bindingClassName`, `graphPropertyName`, `graphFactoryFunName`) are computed on the data class itself rather than in generators.
Two reasons:

1. They are pure functions of `baseName` and `packageName`. Computing them once at parse time keeps the generators free of naming logic.
2. The naming convention is the contract between this processor and the consumer project. Goldens fix the names; tests fail when they drift. Centralising the convention
   on the data class makes "what does this file get called" a single grep.

## UiBindingData

`UiBindingData` is a single data class plus a `UiBindingKind` enum (`Screen`, `Sheet`, or `Tab`).

```kotlin
internal data class UiBindingData(
    val kind: UiBindingKind,
    val composableFunction: MemberName,
    val presenterClass: ClassName,
    val packageName: String,
    val parentScope: ClassName,
)
```

`composableFunction` is a KotlinPoet `MemberName` rather than a `ClassName` because the generated code calls the composable as a top level function, and `MemberName` is
what KotlinPoet's `%M` interpolation expects. Carrying it pre formed avoids re deriving it inside the generator.

The `kind` enum is what `UiBindingGenerator` switches on to pick the content type (`ScreenContent` for `Screen` and `Tab`, `SheetContent` for `Sheet`), the destination
cast target (`ScreenDestination<*>` for `Screen`, `SheetDestination<*>` for `Sheet`, `TabChild<*>` for `Tab`), and whether the composable receives a `Modifier` parameter
(yes for `Screen` and `Tab`, no for `Sheet`). The switch lives inside the generator as a private `Variant` data class; see [generators.md](generators.md).

## AppRootData

`AppRootData` is a single data class produced by [parseAppRootData](parsers.md#approotparser).

```kotlin
internal data class AppRootData(
    val implClassName: ClassName,
    val interfaceClassName: ClassName,
    val factoryClassName: ClassName,
    val factoryFunctionName: String,
    val parentScope: ClassName,
    val packageName: String,
)
```

The generator emits a `@BindingContainer @ContributesTo(parentScope) object <InterfaceName>BindingContainer` containing one `@Provides @SingleIn(parentScope)` function
that takes a `ComponentContext` and the nested factory, and returns the bound interface. Every name the generator needs (the binding object name, the provide function
name) is derived on the data class:

```kotlin
val bindingClassName: ClassName =
    ClassName(packageName, "${interfaceClassName.simpleName}BindingContainer")
val provideFunName: String =
    "provide${interfaceClassName.simpleName}"
```

The factory's single function name (`factoryFunctionName`) is captured at parse time rather than hardcoded so a non standard factory name (`build`, `make`, etc.) works
without generator changes.

## AppRootUiData

`AppRootUiData` is a single data class plus an `AppRootUiParameter` data class for each non-modifier parameter on the annotated composable.

```kotlin
internal data class AppRootUiData(
    val composableFunction: MemberName,
    val packageName: String,
    val parameters: List<AppRootUiParameter>,
    val hasModifier: Boolean,
    val parentScope: ClassName,
)

internal data class AppRootUiParameter(
    val name: String,
    val type: TypeName,
)
```

`composableFunction` is a `MemberName` for the same reason as on `UiBindingData`: the generated code calls the composable as a top level function through KotlinPoet's
`%M` interpolation. `parameters` carries the composable's non-modifier parameters in declaration order; the generator turns each entry into a `val` on the generated
`AppRootProvider` interface and uses the same name to call the composable inside the generated extension. `hasModifier` records whether the composable accepts a
`modifier: Modifier` parameter, so the generator can decide whether to forward the receiver's modifier.

## ChildPresenterData

`ChildPresenterData` is a single data class produced by [parseChildPresenterData](parsers.md#childpresenterparser).

```kotlin
internal data class ChildPresenterData(
    val presenterClass: ClassName,
    val baseName: String,
    val packageName: String,
    val scope: ClassName,
    val parentScope: ClassName,
)
```

The generator emits a `@GraphExtension(scope) interface <BaseName>ChildGraph` exposing the presenter as a property and a nested
`@ContributesTo(parentScope) @GraphExtension.Factory` interface whose single function returns the graph. The derived properties on the data class fix the naming:

```kotlin
val graphClassName: ClassName = ClassName(packageName, "${baseName}ChildGraph")
val graphFactoryFunName: String = "create${baseName}Graph"
val graphPropertyName: String = baseName.replaceFirstChar { it.lowercaseChar() } + "Presenter"
```

The factory function name embeds `baseName` (rather than a constant `createGraph`) so two child graphs contributing to the same parent scope do not collide. Two
`@GraphExtension.Factory` interfaces contributed to one scope merge into Metro's parent graph, and a name clash on the factory function would surface as an
`Incompatible return types` compile error at the activity graph.

## Why an intermediate value at all

The generators target KotlinPoet `FileSpec` outputs. KSP types like `KSClassDeclaration` carry resolution state, lazy children, and a lifetime tied to the round.
Translating once at the parser boundary means:

- Generators are pure functions of the intermediate value: easy to test, easy to read, easy to golden.
- Parsers concentrate KSP specific logic (annotation argument extraction, nested declaration walks) in one place.
- The structure of the intermediate value is the contract between the two halves of the processor. The file under `data/` is the place to look when wiring a new
  annotation through the pipeline.
