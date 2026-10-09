# Data model

The data model is the set of typed values that sit between the parsers and the KotlinPoet generators. Parsers never hand KSP types to a generator. Each parser produces
one of these values, and the generators read nothing else. They live in `codegen/processor/src/main/kotlin/io/github/thomaskioko/codegen/processor/data/`.

There are five top level types:

- The presenter annotation `@NavDestination` resolves to a `NavData`.
- The UI renderer annotations (`@ScreenUi`, `@SheetUi`, `@TabUi`) resolve to a `UiBindingData`.
- The application root presenter annotation (`@AppRoot`) resolves to an `AppRootData`.
- The application root composable annotation (`@AppRootUi`) resolves to an `AppRootUiData`.
- The annotation for a child presenter owned by a parent presenter (`@ChildPresenter`) resolves to a `ChildPresenterData`.

The five are independent and share no supertype, because the generators that consume them produce structurally different output.

## NavData

`NavData` is a sealed interface with two implementations: `ScreenData` for stack screens and modal overlays, and `TabData` for top level tab roots.

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

`ScreenData` carries the route class, an optional `@AssistedFactory` `ClassName`, the route property name (when the presenter takes a runtime parameter), and a
`ScreenKind` enum (`SCREEN` or `OVERLAY`). The binding generator uses the enum to pick `NavDestination.Screen` or `NavDestination.Overlay`. The `factory` field marks
a parameterized presenter. When `factory` is `null`, the presenter uses plain `@Inject` and the generated graph exposes the presenter directly. When `factory` is non `null`,
the presenter uses `@AssistedInject` and the generated graph exposes the factory. The derived `isParameterized` flag reads `factory != null`.

`TabData` carries the route plus a `configEnclosing` field for nested route classes. It has no factory branch, because tabs always use plain `@Inject`. The parser rejects
`@AssistedInject` tab presenters explicitly; see [parsers.md](parsers.md).

`graphPropertyName` and `graphPropertyType` are the one place where screens and tabs produce different output further down. For a parameterized presenter, `ScreenData`
resolves them to the factory's name and class. Otherwise it resolves them to the presenter's name and class. `TabData` always resolves to the
presenter.

### Naming on the data class

We compute the derived properties (`graphClassName`, `bindingClassName`, `graphPropertyName`, `graphFactoryFunName`) on the data class, not in the generators.
There are two reasons:

1. They are pure functions of `baseName` and `packageName`. Computing them once at parse time keeps naming logic out of the generators.
2. The naming convention is the contract between this processor and the consumer project. Goldens pin the names, and tests fail when they drift. With the convention
   on the data class, "what is this file called" is a single grep.

## UiBindingData

`UiBindingData` is one data class plus a `UiBindingKind` enum (`Screen`, `Sheet`, or `Tab`).

```kotlin
internal data class UiBindingData(
    val kind: UiBindingKind,
    val composableFunction: MemberName,
    val presenterClass: ClassName,
    val packageName: String,
    val parentScope: ClassName,
)
```

`composableFunction` is a KotlinPoet `MemberName`, not a `ClassName`. The generated code calls the composable as a top level function, and `MemberName` is
what KotlinPoet's `%M` interpolation expects. Building it once here saves the generator from deriving it again.

`UiBindingGenerator` switches on `kind` to pick three things. The content type is `ScreenContent` for `Screen` and `Tab`, and `SheetContent` for `Sheet`. The destination
cast target is `ScreenDestination<*>` for `Screen`, `SheetDestination<*>` for `Sheet`, and `TabChild<*>` for `Tab`. The composable gets a `Modifier` parameter
for `Screen` and `Tab`, but not for `Sheet`. The switch lives inside the generator as a private `Variant` data class; see [generators.md](generators.md).

## AppRootData

`AppRootData` is one data class, produced by [parseAppRootData](parsers.md#approotparser).

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

The generator emits a `@BindingContainer @ContributesTo(parentScope) object <InterfaceName>BindingContainer`. It holds one `@Provides @SingleIn(parentScope)` function
that takes a `ComponentContext` and the nested factory, and returns the bound interface. Every name the generator needs (the binding object name, the provide function
name) is derived on the data class:

```kotlin
val bindingClassName: ClassName =
    ClassName(packageName, "${interfaceClassName.simpleName}BindingContainer")
val provideFunName: String =
    "provide${interfaceClassName.simpleName}"
```

We capture the factory's function name (`factoryFunctionName`) at parse time instead of hardcoding it. That way a non standard factory name (`build`, `make`, etc.) works
without touching the generator.

## AppRootUiData

`AppRootUiData` is one data class, plus an `AppRootUiParameter` data class for each parameter on the annotated composable other than the modifier.

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
`%M` interpolation. `parameters` holds the composable's parameters other than the modifier, in declaration order. The generator turns each entry into a `val` on the
generated `AppRootProvider` interface, and uses the same name when it calls the composable inside the generated extension. `hasModifier` records whether the composable
takes a `modifier: Modifier` parameter, so the generator knows whether to forward the receiver's modifier.

## ChildPresenterData

`ChildPresenterData` is one data class, produced by [parseChildPresenterData](parsers.md#childpresenterparser).

```kotlin
internal data class ChildPresenterData(
    val presenterClass: ClassName,
    val baseName: String,
    val packageName: String,
    val scope: ClassName,
    val parentScope: ClassName,
)
```

The generator emits a `@GraphExtension(scope) interface <BaseName>ChildGraph` that exposes the presenter as a property. Inside it is a nested
`@ContributesTo(parentScope) @GraphExtension.Factory` interface whose single function returns the graph. The derived properties on the data class fix the names:

```kotlin
val graphClassName: ClassName = ClassName(packageName, "${baseName}ChildGraph")
val graphFactoryFunName: String = "create${baseName}Graph"
val graphPropertyName: String = baseName.replaceFirstChar { it.lowercaseChar() } + "Presenter"
```

The factory function name includes `baseName` (instead of a fixed `createGraph`) so two child graphs in the same parent scope do not collide. Metro merges every
`@GraphExtension.Factory` interface contributed to a scope into the parent graph. If two factory functions had the same name, you would get an
`Incompatible return types` compile error at the activity graph.

## Why an intermediate value at all

The generators produce KotlinPoet `FileSpec` outputs. KSP types like `KSClassDeclaration` carry resolution state and lazy children, and they only live for one round.
We translate once, at the parser boundary, which gives us three things:

- Generators are pure functions of the intermediate value. They are easy to test, easy to read, and easy to golden.
- KSP specific logic (reading annotation arguments, walking nested declarations) stays in the parsers.
- The intermediate value is the contract between the two halves of the processor. When you wire a new annotation through the pipeline, start with the file
  under `data/`.
