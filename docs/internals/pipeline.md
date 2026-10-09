# Pipeline

The processor's entry point is `NavigationCodegenProcessor` in `codegen/processor/src/main/kotlin/io/github/thomaskioko/codegen/processor/NavigationCodegenProcessor.kt`.
KSP loads it through `NavigationCodegenProcessorProvider` and calls `process(resolver)` once per round.

## One round

A KSP round is one pass of the processor over the current source set. KSP can run several rounds when other processors produce new symbols. This codegen never
produces input for itself, so it runs once per build and returns an empty list of deferred symbols.

Each call runs seven annotation queries in order. We keep the fully qualified names as `const val` declarations in `Constants.kt`, so the processor and its tests read
the same values.

```kotlin
override fun process(resolver: Resolver): List<KSAnnotated> {
    processNavDestination(resolver)
    processUiBinding(resolver, Constants.SCREEN_UI_FQN, Constants.SCREEN_UI, UiBindingKind.Screen)
    processUiBinding(resolver, Constants.SHEET_UI_FQN, Constants.SHEET_UI, UiBindingKind.Sheet)
    processUiBinding(resolver, Constants.TAB_UI_FQN, Constants.TAB_UI, UiBindingKind.Tab)
    processAppRoot(resolver)
    processAppRootUi(resolver)
    processChildPresenter(resolver)
    return emptyList()
}
```

Five private helpers cover the kinds of annotated symbol the processor knows. `processNavDestination` and `processUiBinding` handle the navigation annotations. Three
named helpers (`processAppRoot`, `processAppRootUi`, `processChildPresenter`) handle the standalone class and function targets. All five do the same steps: read the
matching symbols from KSP, type check each one, pass it to a parser, and send the parser's result to a generator. They differ only in the symbol kind they accept and the
data type they produce.

## Five paths

`processNavDestination` is the path for `@NavDestination`. KSP returns every class declaration with the annotation, and the helper hands each one to
`parseNavDestinationData`, which returns a `NavData?`. A null means the parser already logged a compile error, so the helper skips the symbol. A non null result
produces two files: the graph from `ScreenGraphGenerator`, and the binding from either `NavDestinationBindingGenerator` or `TabDestinationBindingGenerator`. Which binding
generator runs depends on whether the parse produced a `ScreenData` or a `TabData`. `writeFiles` writes both through KSP's `CodeGenerator`.

`processUiBinding` is the path for function targets that produce a `UiBindingData`. `@ScreenUi`, `@SheetUi`, and `@TabUi` all use it. The caller tells them apart by
passing a `UiBindingKind` value. KSP returns every function declaration with the annotation, and the helper hands each one to `parseUiBindingData`, which returns a
`UiBindingData?`. A non null result goes straight to `UiBindingGenerator.generate(data)`. There is no router here, because each annotation produces exactly one file.

`processAppRoot` is the path for `@AppRoot`. KSP returns every class declaration with the annotation, and the helper hands each one to `parseAppRootData`, which returns an
`AppRootData?`. A non null result goes straight to `AppRootBindingGenerator.generate(data)`. We keep this data type separate from `NavData` because the generated binding
container has a different structure from a destination binding. It is a `@BindingContainer object` that exposes the bound interface, not an interface plus companion
that contributes into a multibinding.

`processAppRootUi` is the path for `@AppRootUi`. KSP returns every function declaration with the annotation. The helper rejects more than one annotated function per
round and reports a compile error on the duplicate. A single annotation goes to `parseAppRootUiData`, which returns an `AppRootUiData?`. A non null result goes
straight to `AppRootUiBindingGenerator.generate(data)`. We keep this data type separate from `UiBindingData` because the generated artifact is a provider interface plus an
extension, not a `ScreenContent` or `SheetContent` multibinding entry.

`processChildPresenter` is the path for `@ChildPresenter`. KSP returns every class declaration with the annotation, and the helper hands each one to
`parseChildPresenterData`, which returns a `ChildPresenterData?`. A non null result goes to `ScreenGraphGenerator.generate(data)`. That is the same generator the destination
path uses, because a child graph has the same shape as a screen graph. `ChildPresenterData` implements `GraphData` and not `NavData`. The generated artifact is a
graph extension that exposes one presenter, with no destination binding next to it.

## File writing

Every helper calls one `writeFiles` function. It takes the annotated `KSDeclaration`, so class and function symbols share it. It builds one
`Dependencies(aggregating = false, containingFile)` and calls `FileSpec.writeTo(codeGenerator, deps)` for each file.

`aggregating = false` is the detail that matters. It tells KSP that a generated file depends only on the source file its annotation lives in. KSP can then reprocess one
feature when its source changes, without invalidating the generated output of the other features. See the [glossary](index.md#glossary) for the term.

Symbols whose containing file cannot be resolved are skipped with a warning. These are usually symbols another processor created in the same round. This branch never
fires in normal use. We keep the warning so a future interaction with another processor does not drop output silently.

## Errors

The processor never throws on user error. Every validation failure in a parser calls `logger.error(message, offendingSymbol)` and returns `null`, and the helper treats
that as "skip this symbol." KSP turns the logged error into a compile error at the symbol's source position. You see it in the IDE next to any other compile
errors.

The `ErrorPathTest` suite in `processor-test/` runs every branch that produces an error. It asserts on the compiler messages instead of a golden file. See
[testing.md](testing.md) for how it is set up.
