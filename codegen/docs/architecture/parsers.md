# Parsers

The parser layer maps KSP symbols into the typed intermediate values described in [data-model.md](data-model.md). Two parser files live under
`codegen/processor/src/main/kotlin/io/github/thomaskioko/codegen/processor/parser/`:

- `NavDestinationParser.kt` covers the `@NavDestination` path for all three destination kinds.
- `UiParser.kt` covers `@ScreenUi` and `@SheetUi`.

Both parsers share `AnnotationArguments.kt`, a small set of KSP extension helpers.

## Annotation argument helpers

`AnnotationArguments.kt` is the only place KSP's `KSAnnotation` types are picked apart. It exposes seven extensions:

- `findArgument(name)` reads an annotation argument by name, falling back to declared defaults. It throws if the argument is missing both an explicit value and a default.
  That throw indicates the annotation class itself was changed without updating the parser, which is a programming error in this repo. User errors go through `KSPLogger`
  instead, never through throws.
- `classArgument(name)` extracts a `KClass<*>` argument as a KotlinPoet `ClassName`.
- `enumArgument(name)` reads an enum reference as the simple name of the selected constant. KSP can represent enum arguments in three different forms (`KSType`,
  `KSClassDeclaration`, raw `String`); this helper normalises them.
- `findAnnotation(fqn)`, `hasAnnotation(fqn)`, `findNestedAssistedFactory()`, and `hasAssistedAnnotation()` are short lookups for the patterns the parsers repeat: find
  the matching annotation by fully qualified name, find a nested `@AssistedFactory`, check whether a constructor parameter carries `@Assisted`.

Keeping these in one file means the parsers themselves read as straight line transformations with no KSP boilerplate.

## NavDestinationParser

`parseNavDestinationData` is the primary parser. It reads `route`, `parentScope`, and `kind` from `@NavDestination`, then branches on the kind:

- `SCREEN` and `OVERLAY` route through `parseScreenLike`, which produces a `ScreenData` tagged with the matching `ScreenKind`.
- `TAB_ROOT` routes through `parseTabLike`, which produces a `TabData`.
- An unknown kind is reported as a compile error and the parser returns `null`.

`parseScreenLike` looks for a nested `@AssistedFactory` on the presenter. If one is present, the presenter is parameterized: the parser confirms the presenter has exactly
one `@Assisted` constructor parameter through `inferSingleRoutePropertyForNavDestination`, captures the parameter's name as the route property, and stores the factory's
class name. The wrong number of `@Assisted` parameters (zero, or two or more) is a compile error pointing at the presenter declaration. If no nested factory is present,
the presenter is plain `@Inject` and the resulting `ScreenData` has no factory or route property.

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

A tab's route is a singleton `data object` that carries no payload, so there is no value for the parser to thread from the route into the presenter at navigation time.
Allowing a tab to accept a runtime parameter would mean the host needs a side channel to recover the parameter when the process is killed and the navigation state is
later restored, which defeats the polymorphic save and restore the rest of the codegen is built around.

## UiParser

`parseUiBindingData` is shared by `@ScreenUi` and `@SheetUi`. The caller passes a `UiBindingKind` (`Screen` or `Sheet`) and the parser configures itself accordingly. It
rejects functions in the default package (the generated binding needs a non empty package to land in), records the function as a `MemberName`, reads `presenter` and
`parentScope` as `ClassName` instances, and returns a `UiBindingData`.

There is no `@AssistedInject` detection branch on the UI side. The generated binding has the same structure for both `@ScreenUi` and `@SheetUi`. The fields that vary by
kind (the content type, the destination cast target, whether to forward `Modifier`) are picked at generation time, not at parse time. See [generators.md](generators.md).

## Error reporting

Every parser path that fails calls `logger.error(message, offendingSymbol)` and returns `null`. KSP turns those into compile errors at the symbol's source position. The
`ErrorPathTest` suite in `processor-test/` exercises each branch; see [testing.md](testing.md). When you add a new validation rule, add the matching branch to
`ErrorPathTest` so a regression that drops the rule fails loudly.
