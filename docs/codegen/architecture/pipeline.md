# Pipeline

The processor entry point is `NavigationCodegenProcessor` in `codegen/processor/src/main/kotlin/io/github/thomaskioko/codegen/processor/NavigationCodegenProcessor.kt`.
KSP loads it through `NavigationCodegenProcessorProvider` and calls `process(resolver)` once per round.

## One round

A KSP round is one pass of the processor over the current source set. KSP can run multiple rounds when other processors produce new symbols, but this codegen never
produces input for itself, so it runs once per build and returns an empty list of deferred symbols.

Each call walks seven annotation queries in order. The fully qualified names are kept as `const val` declarations in `Constants.kt` so the processor and its tests share a
single source of truth.

```kotlin
override fun process(resolver: Resolver): List<KSAnnotated> {
    processAnnotation(resolver, Constants.NAV_DESTINATION_FQN, Constants.NAV_DESTINATION) { presenter ->
        parseNavDestinationData(presenter, logger)
    }
    processUiBinding(resolver, Constants.SCREEN_UI_FQN, Constants.SCREEN_UI, UiBindingKind.Screen)
    processUiBinding(resolver, Constants.SHEET_UI_FQN, Constants.SHEET_UI, UiBindingKind.Sheet)
    processUiBinding(resolver, Constants.TAB_UI_FQN, Constants.TAB_UI, UiBindingKind.Tab)
    processAppRoot(resolver)
    processAppRootUi(resolver)
    processChildPresenter(resolver)
    return emptyList()
}
```

Five private helpers cover the kinds of annotated symbol the processor recognises. `processAnnotation` and `processUiBinding` cover the navigation annotations; three
named helpers (`processAppRoot`, `processAppRootUi`, `processChildPresenter`) cover the standalone class and function targets. All five follow the same steps: read the
matching symbols from KSP, type check each one, pass it to a parser, route the parser's result to a generator. They differ only in the symbol kind they accept and the
data type they produce.

## Five paths

`processAnnotation` is the path for class targets that produce a `NavData`. Currently only `@NavDestination` uses it. KSP returns every class declaration carrying the
annotation; the helper hands each to the parser, which returns a `NavData?`. A null return means the parser already logged a compile error and the helper skips the
symbol. A non null return goes to `FileGenerator.generate(data)`, which produces the graph file and the binding file. `writeFiles` writes them through KSP's
`CodeGenerator`.

`processUiBinding` is the path for function targets that produce a `UiBindingData`. `@ScreenUi`, `@SheetUi`, and `@TabUi` all use it, distinguished by a `UiBindingKind`
enum value the caller passes in. KSP returns every function declaration carrying the annotation; the helper hands each to `parseUiBindingData`, which returns a
`UiBindingData?`. A non null result goes directly to `UiBindingGenerator.generate(data)` (there is no router wrapper because each annotation produces exactly one file).

`processAppRoot` is the path for `@AppRoot`. KSP returns every class declaration carrying the annotation; the helper hands each to `parseAppRootData`, which returns an
`AppRootData?`. A non null result goes directly to `AppRootBindingGenerator.generate(data)`. The data type is independent of `NavData` because the generated binding
container has a different structure than a destination binding (it is a `@BindingContainer object` that exposes the bound interface, not an interface plus companion
contributing into a multibinding).

`processAppRootUi` is the path for `@AppRootUi`. KSP returns every function declaration carrying the annotation; the helper rejects more than one annotated function per
round and reports a compile error pointing at the duplicate. A single annotation goes to `parseAppRootUiData`, which returns an `AppRootUiData?`. A non null result goes
directly to `AppRootUiBindingGenerator.generate(data)`. The data type is independent of `UiBindingData` because the generated artifact is a provider interface plus an
extension, not a `ScreenContent` or `SheetContent` multibinding entry.

`processChildPresenter` is the path for `@ChildPresenter`. KSP returns every class declaration carrying the annotation; the helper hands each to
`parseChildPresenterData`, which returns a `ChildPresenterData?`. A non null result goes directly to `ChildGraphGenerator.generate(data)`. The data type is independent of
`NavData` because the generated artifact is a graph extension exposing a single presenter, with no destination binding entry alongside it.

## File writing

Both `writeFiles` (for class symbols) and `writeFunctionFiles` (for function symbols) build a single `Dependencies(aggregating = false, containingFile)` and call
`FileSpec.writeTo(codeGenerator, deps)` for each emitted file.

`aggregating = false` is the important detail. It tells KSP that the generated file depends only on the source file the annotation lives in. KSP can therefore reprocess a
single feature when its source changes without invalidating the generated output for sibling features. See the [glossary](index.md#glossary) for the term.

Symbols whose containing file cannot be resolved (typically symbols synthesised by another processor in the same round) are skipped with a warning. This branch never
fires in normal use; the warning exists so a future processor interaction does not silently drop output.

## Where errors surface

The processor never throws on user error. Every validation failure in a parser calls `logger.error(message, offendingSymbol)` and returns `null`, which the helper treats
as "skip this symbol." KSP turns the logged error into a compile error at the symbol's source position so the user sees it in their IDE alongside any other compile
errors.

The `ErrorPathTest` suite in `processor-test/` exercises every error producing branch and asserts on the compiler messages rather than against a golden file. See
[testing.md](testing.md) for how it is wired up.
