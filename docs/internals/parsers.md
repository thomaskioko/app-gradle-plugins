# Parsers

The parser layer turns KSP symbols into the typed values described in [data-model.md](data-model.md). There are five parser files under
`codegen/processor/src/main/kotlin/io/github/thomaskioko/codegen/processor/parser/`:

- `NavDestinationParser.kt` handles `@NavDestination` for all three destination kinds.
- `UiParser.kt` handles `@ScreenUi`, `@SheetUi`, and `@TabUi`.
- `ChildPresenterParser.kt` handles `@ChildPresenter` on child presenters owned by a parent presenter.
- `AppRootParser.kt` handles `@AppRoot` on the root presenter implementation.
- `AppRootUiParser.kt` handles `@AppRootUi` on the host composable.

All five share `AnnotationArguments.kt`, a small set of KSP extension helpers.

## Annotation argument helpers

`AnnotationArguments.kt` is the only place we pick apart KSP's `KSAnnotation` types. It has seven extensions:

- `findArgument(name)` reads an annotation argument by name and falls back to the declared default. It throws if the argument has neither an explicit value nor a default.
  That throw means someone changed the annotation class without updating the parser, which is a programming error in this repo. User errors go through `KSPLogger`
  instead, never through throws.
- `classArgument(name)` reads a `KClass<*>` argument as a KotlinPoet `ClassName`.
- `enumArgument(name)` reads an enum reference as the simple name of the selected constant. KSP can represent enum arguments in three forms (`KSType`,
  `KSClassDeclaration`, raw `String`), and this helper normalises them.
- `findAnnotation(fqn)`, `hasAnnotation(fqn)`, `findNestedAssistedFactory()`, and `hasAssistedAnnotation()` are short lookups for things the parsers do over and over:
  find an annotation by fully qualified name, find a nested `@AssistedFactory`, check whether a constructor parameter has `@Assisted`.

With these in one file, the parsers read as straight line transformations with no KSP boilerplate.

## NavDestinationParser

`parseNavDestinationData` is the main parser. It reads `route`, `parentScope`, and `kind` from `@NavDestination`, then branches on the kind:

- `SCREEN` and `OVERLAY` go through `parseScreenLike`, which produces a `ScreenData` tagged with the matching `ScreenKind`.
- `TAB_ROOT` goes through `parseTabLike`, which produces a `TabData`.
- An unknown kind is reported as a compile error, and the parser returns `null`.

`parseScreenLike` looks for a nested `@AssistedFactory` on the presenter. If there is one, the presenter is parameterized. The parser then checks, through
`inferSingleRoutePropertyForNavDestination`, that the presenter has exactly one `@Assisted` constructor parameter. It records that parameter's name as the route property
and stores the factory's class name. Any other number of `@Assisted` parameters (zero, or two or more) is a compile error on the presenter declaration. If there is no
nested factory, the presenter is plain `@Inject` and the resulting `ScreenData` has no factory or route property.

`parseTabLike` rejects `@AssistedInject` tab presenters explicitly:

```kotlin
if (presenter.findNestedAssistedFactory() != null) {
    logger.error(
        "@${Constants.NAV_DESTINATION}(kind = TAB_ROOT) does not support @AssistedInject " +
            "presenters; tab roots must be plain @Inject",
        presenter,
    )
    return null
}
```

A tab's route is a singleton `data object` with no payload. So there is no value to pass from the route into the presenter at navigation time. If a tab could take a
runtime parameter, the host would need some other way to recover it after the process is killed and the navigation state is restored. That breaks the polymorphic save
and restore the rest of the codegen is built around.

## UiParser

`parseUiBindingData` is shared by `@ScreenUi`, `@SheetUi`, and `@TabUi`. The caller passes a `UiBindingKind` (`Screen`, `Sheet`, or `Tab`), and the parser configures itself
from it. It rejects functions in the default package, because the generated binding needs a non empty package to live in. It records the function as a `MemberName`, reads
`presenter` and `parentScope` as `ClassName` instances, and returns a `UiBindingData`.

The UI side has no `@AssistedInject` detection branch. The generated binding has the same structure for all three kinds. The parts that change by kind (the content
type, the destination cast target, whether to forward `Modifier`) are picked at generation time, not at parse time. See [generators.md](generators.md).

## ChildPresenterParser

`parseChildPresenterData` reads `@ChildPresenter` and records the annotated class as a `ClassName`. It derives the base name (the simple name without the `Presenter`
suffix) and reads `scope` and `parentScope` as `ClassName` instances. It also looks for a nested `@AssistedFactory` with `findNestedAssistedFactory()`. When it finds
one, it records the factory's `ClassName` in `factory`, and the generated graph exposes the factory instead of the presenter. It returns a `ChildPresenterData`, and the
generator uses its derived properties (`graphClassName`, `graphFactoryFunName`, `graphPropertyType`, `graphPropertyName`) as they are.

We keep this parser deliberately small. It reports no errors of its own. A missing nested factory just means a plain `@Inject` presenter. The parent passes any
runtime values to the factory's `create(...)` call, not through a route, so there is no route property to read. The processor entry already checks that the
annotated symbol is a class before it calls the parser, so the parser does not check the symbol kind again.

## AppRootParser

`parseAppRootData` reads `@AppRoot`, validates the annotated class, and returns an `AppRootData`. Validation has three steps:

1. The class must have `@AssistedInject`. Without it you get a compile error on the class declaration. `@AppRoot` does not generate Metro injection. It only generates
   the binding container that calls the assisted factory, and Metro still needs `@AssistedInject` on the class to create it.
2. The class must declare a nested `@AssistedFactory` interface with exactly one function. We record that function's name (usually `create`) for the generated
   provider body.
3. The class must extend exactly one interface that is not a marker. `inferBoundInterface` walks the resolved supertypes, skips `kotlin.Any` and `com.arkivanov.decompose.ComponentContext`,
   and checks that exactly one candidate is left. Zero is a compile error (the consumer forgot to declare the bound interface). More than one is also a compile error (the
   processor cannot choose).

The generated provider returns the bound interface. We use its name to derive the binding object name (`<InterfaceName>BindingContainer`)
and the provide function name (`provide<InterfaceName>`). These names are part of the contract with the consumer, and goldens pin them.

## AppRootUiParser

`parseAppRootUiData` reads `@AppRootUi`, validates the annotated function, and returns an `AppRootUiData`. The parser walks the function's parameters in declaration
order. It looks for a final parameter named `modifier` of type `androidx.compose.ui.Modifier`, and treats every other parameter as a graph dependency. The graph
dependencies become properties on the generated `AppRootProvider` interface. The modifier, when there is one, is forwarded by the generated extension.

The type of the first parameter that is not the modifier must equal the annotation's `presenter` argument. A mismatch is a compile error. It means someone changed
the annotation or the composable without updating the other. The check is shallow (it compares canonical names as strings), but it catches the usual case of a presenter rename.

Only one `@AppRootUi` function is allowed per round. The processor entry enforces this, not the parser. The duplicate check lives in `processAppRootUi`, because it
tracks state across symbols within one round. That keeps the parser a pure function.

## Error reporting

Every failing parser path calls `logger.error(message, offendingSymbol)` and returns `null`. KSP turns those calls into compile errors at the symbol's source position. The
`ErrorPathTest` suite in `processor-test/` runs each branch; see [testing.md](testing.md). When you add a validation rule, add a matching branch to
`ErrorPathTest`, so a change that drops the rule fails loudly.
