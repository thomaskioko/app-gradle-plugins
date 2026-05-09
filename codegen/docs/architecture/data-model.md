# Data model

Parsers do not feed KSP types directly to KotlinPoet generators. Every parser produces a typed intermediate value and the generators consume nothing else. This
intermediate value lives in `codegen/processor/src/main/kotlin/io/github/thomaskioko/codegen/processor/data/`.

There are two top level types. Presenter side annotations (`@NavDestination`) resolve to a `NavData`. UI renderer annotations (`@ScreenUi`, `@SheetUi`) resolve to a
`UiBindingData`. The two are independent and share no supertype because the downstream generators are structurally different.

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

`UiBindingData` is a single data class plus a `UiBindingKind` enum (`Screen` or `Sheet`).

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

The `kind` enum is what `UiBindingGenerator` switches on to pick the content type (`ScreenContent` or `SheetContent`), the destination cast target (`ScreenDestination<*>`
or `SheetDestination<*>`), and whether the composable receives a `Modifier` parameter. The switch lives inside the generator as a private `Variant` data class; see
[generators.md](generators.md).

## Why an intermediate value at all

The generators target KotlinPoet `FileSpec` outputs. KSP types like `KSClassDeclaration` carry resolution state, lazy children, and a lifetime tied to the round.
Translating once at the parser boundary means:

- Generators are pure functions of the intermediate value: easy to test, easy to read, easy to golden.
- Parsers concentrate KSP specific logic (annotation argument extraction, nested declaration walks) in one place.
- The structure of the intermediate value is the contract between the two halves of the processor. The file under `data/` is the place to look when wiring a new
  annotation through the pipeline.
