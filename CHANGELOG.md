Change Log
==========

## 0.8.1 *(2026-06-20)*

- `ignoreAll()` now defaults to the project it is called from when no path is passed, so a bare `ignoreAll()` inside a module's `scaffold {}` block silences that module's dependency analysis. The empty vararg previously silenced nothing, which left framework umbrella modules such as `ios-framework` flagged for every dependency the iOS application consumes across the Objective-C boundary.
- Exclude `codegen-featureflag-annotations` from the unused-dependency analysis. The `@FeatureFlag` annotation has source retention and is consumed by KSP, so dependency analysis cannot see it in bytecode and reported it as unused on every module that calls `useFeatureFlagCodegen()`.

## 0.8.0 *(2026-05-19)*

- Introduce a KSP-based feature-flag codegen that eliminates the per-flag qualifier and DI binding boilerplate in Metro KMP projects. A single `@FeatureFlag` object anchor generates the Metro `@Qualifier` and the `Set<FeatureFlag<Boolean>>` binding, and a `platform` field scopes a flag to one platform at compile time.
- Bump Kotlin to 2.4.0.
- Refactor dependency analysis exclusions to a DSL-based configuration
- `tvmaniac:no-manual-nav-binding` flags manual bindings  `@Provides @IntoSet` providers returning
  `NavRoot`, `NavRootBinding`, `NavDestination`, or `NavRouteBinding`. The codegen processor owns
  those multibindings whenever a presenter is annotated with `@NavDestination` or `@AppRoot`, so
  any hand-written contribution duplicates the generated provider.

## 0.7.9 *(2026-05-17)*

- Fix dependency analysis configuration

## 0.7.8 *(2026-05-10)*

### Navigation

Three additions extend the codegen surface so consumers no longer hand-write tab pager renderers, parent-owned child presenter graphs, or the `NavRoot` multibinding.

- `@TabUi` targets a `@Composable` function as the renderer for one tab pager page. The generated `ScreenContent` binding mirrors `@ScreenUi` except the matcher casts the active child to `TabChild<*>` rather than `ScreenDestination<*>`. Use it on bottom-bar tab pages whose presenters are wrapped as `TabChild` rather than pushed onto a stack.
- `@ChildPresenter` targets a presenter constructed by another presenter rather than navigated to through a route. The processor emits a `<Presenter>ChildGraph` graph extension exposing the presenter as a property plus a `@ContributesTo(parentScope) @GraphExtension.Factory` whose `create<BaseName>Graph` function takes a `ComponentContext` and returns the graph. Multiple children may share a `scope`; each gets its own graph extension and its own factory function (the unique name avoids return-type collisions when both factories contribute to the same parent scope).
- `@NavDestination(kind = TAB_ROOT)` now also contributes the route singleton itself into `Set<NavRoot>`. Consumers no longer need to keep a hand-written `<Feature>RootBinding` next to each tab to populate that set. The new contribution lives inside the existing `<Presenter>TabDestinationBinding` file alongside the `NavDestination<*>` and `NavRootBinding<*>` entries.

See [annotations.md](codegen/docs/annotations.md), [examples.md](codegen/docs/examples.md) sections 4, 7, and 8, and [architecture/consumer-contract.md](codegen/docs/architecture/consumer-contract.md) for the full surface.

### Lint rules

- `tvmaniac:presenter-needs-codegen-annotation` now accepts `@ChildPresenter` as a satisfying annotation, so child presenters routed through the new codegen path no longer need an `editorconfig` exemption.

### Fix
- Apply `com.autonomousapps.dependency-analysis` per subproject.
- Default `appAuthRedirectScheme` test manifest placeholder on KMP Android targets so transitive AppAuth dependencies do not break `processAndroidHostTestManifest` / `processAndroidDeviceTestManifest`.

## 0.7.7 *(2026-05-10)*

### Navigation

Two new annotations cover the application's root host. The pair eliminates the manual `@BindingContainer` that consumers used to write for the root presenter and the multi-argument call site every activity used to make to render the root composable.

- `@AppRoot` targets an `@AssistedInject` presenter implementation. The processor reads the nested `@AssistedFactory`, infers the bound interface from the implementation's supertypes, and emits a `<InterfaceName>BindingContainer` that contributes `@Provides @SingleIn(parentScope)` for the bound interface. Consumers replace their hand-written root binding container with one annotation.
- `@AppRootUi` targets the host `@Composable` function. The processor reads the function's non-modifier parameters and emits an `AppRootProvider` interface plus a `@Composable AppRootProvider.AppRootContent(modifier)` extension. Consumers make their activity-scope `@DependencyGraph` extend the generated `AppRootProvider`, and the activity invokes `graph.AppRootContent()` instead of forwarding each dependency by hand.

The codegen now publishes `@SingleIn`, `@Composable`, and `Modifier` in addition to the previously-published Metro and Decompose constants. See [annotations.md](codegen/docs/annotations.md), [examples.md](codegen/docs/examples.md) sections 7 and 8, and [architecture/consumer-contract.md](codegen/docs/architecture/consumer-contract.md) for the full surface.

### Lint rules

Two new ktlint rules enforce that the codegen annotations above are actually applied. Forgetting either annotation now fails the build at lint time rather than slipping through to runtime.

- `tvmaniac:presenter-needs-codegen-annotation` flags any top-level class whose name ends with `Presenter` and is annotated with `@Inject` or `@AssistedInject` but is missing both `@NavDestination` and `@AppRoot`. Classes carrying a `@Contributes...` Metro annotation are exempt; child presenters routed through a manual `@GraphExtension` opt out via the new `ktlint_tvmaniac_unrouted_presenters` editorconfig property.
- `tvmaniac:compose-screen-needs-codegen-annotation` flags any top-level `@Composable` function with a `presenter` or `rootPresenter` parameter that is missing `@ScreenUi`, `@SheetUi`, and `@AppRootUi`. Tab-root screens dispatched manually inside a parent host opt out via the new `ktlint_tvmaniac_unrouted_screens` editorconfig property.


### KSP

- Register the `kspCommonMainKotlinMetadata` output (`build/generated/ksp/metadata/commonMain/kotlin`) as a `commonMain` Kotlin source directory for KMP projects, and wire every `KotlinCompilationTask` and per-target `ksp*` task to depend on it. Resolves IDE `Unresolved reference` warnings for KSP-generated symbols (Metro `@GraphExtension`, navigation codegen) referenced from `commonMain`.
- `useCodegen()` now registers the navigation codegen processor only on `kspCommonMainMetadata` for KMP projects (via the new `addKspDependencyForCommonMain` helper). Combined with the metadata srcDir registration above, this avoids duplicate generation that would otherwise occur when per-target ksp tasks reprocessed commonMain sources.

## 0.7.6 *(2026-05-09)*

### Navigation

A single annotation now covers every navigation destination. The three older annotations (`@NavScreen`, `@TabScreen`, `@NavSheet`)
have been replaced by `@NavDestination(kind = ...)`, and the processor auto-detects whether a presenter
accepts a runtime parameter from the route.

- Replace `@NavScreen`, `@TabScreen`, and `@NavSheet` with a single `@NavDestination(kind = ...)` annotation. The
processor reads one annotation on the presenter and picks the destination role from the `kind` parameter: `SCREEN`
for stack screens, `OVERLAY` for modal sheets and dialogs, `TAB_ROOT` for top level tab anchors.
- Auto-detect parameterized presenters from a nested Metro's `@AssistedFactory`. A presenter with a
nested factory generates a binding that reads the route property and threads it through `factory.create(...)`.
A plain `@Inject` presenter generates a binding that exposes the presenter directly. Annotating a tab presenter with `@AssistedInject` produces a compile error because a tab's route is a singleton `data object` and carries no payload.


### Lint rules

A new published `lint-rules` artifact ships six ktlint rules that enforce the project's navigation, preview, dependency injection, and test naming conventions. Two of the rules read `.editorconfig` so consumer projects can adapt the navigation layer location and the set of forbidden preview wrappers without forking.

- New `lint-rules` artifact (`io.github.thomaskioko.gradle.plugins:lint-rules`) shipping six custom ktlint rules:
    - `tvmaniac:no-mutating-router-import` prevents Decompose router mutation imports outside the navigation layer. The two read only types (`ChildStack`, `ChildSlot`) remain allowed everywhere.
    - `tvmaniac:no-navigation-construct-outside-nav` prevents `StackNavigation()` and `SlotNavigation()` construction outside the navigation layer. Type references in parameter and return positions are unaffected.
    - `tvmaniac:no-custom-navigator-interface` prevents feature specific `*Navigator` interfaces. Presenters must inject the canonical `Navigator` or `SheetNavigator` from `navigation/api`.
    - `tvmaniac:no-style-wrapper-in-preview` prevents redundant styling wrappers (`TvManiacTheme`, `TvManiacBackground`, `Surface`, `MaterialTheme` by default) inside `@Preview` composables. The wrapper provider applies the project styling once.
    - `tvmaniac:metro-redundant-inject` removes redundant `@Inject` from classes that already declare a Metro `@Contributes...` annotation. Autocorrect-able.
    - `tvmaniac:test-name-format` enforces the `should X given Y` test naming convention. Backticked and camelCase forms are both accepted.
- The navigation and preview rules read `.editorconfig` properties (`ktlint_tvmaniac_navigation_module_paths`, `ktlint_tvmaniac_preview_wrappers`, `ktlint_tvmaniac_preview_wrapper_packages`) so consumer projects can adjust the navigation layer location and the set of forbidden wrappers without forking. See [lint-rules/README.md](lint-rules/README.md).

### Misc

- Bumped ktlint dependency 1.4.0 → 1.8.0.
- Dependency updates.

## 0.7.5 *(2026-04-26)*
- Apply [KT-42254](https://youtrack.jetbrains.com/issue/KT-42254) Kotlin/Native cache-disable workaround automatically in `addIosTargetsWithXcFramework`
- `io.github.thomaskioko.gradle.plugins.root` is now **required** on the root project. Any subproject plugin in this suite (`app`, `android`, `jvm`, `multiplatform`, `base`) throws `GradleException` at apply-time when `root` is missing on the root project.
- Add `id("io.github.thomaskioko.gradle.plugins.root")` to the root `build.gradle.kts` plugins block.
- Removed per-subproject application of `com.autonomousapps.dependency-analysis` from `AndroidPlugin`. The plugin is applied only at the root via `RootPlugin`; consumers that previously relied on the leaked transitive must apply `RootPlugin` explicitly.
- Introduced typed `ScaffoldProperties` layer; all Gradle property keys consumed by the plugins are centralized in `PropertyKeys`.
- Aggregate test tasks (`linuxTest`, `iosTest`, `ciTest`) are now registered only on the root project. Subproject plugins (`AndroidPlugin`, `JvmPlugin`, `KotlinMultiplatformPlugin`) attach their variant test tasks to the root aggregates via `dependsOn`.
- Removed dead `SpotlessPlugin.shouldConfigureXmlFormatting` method.
- `AndroidPlugin` no longer throws when the `lint` bundle is absent from the consumer's version catalog. The lint-check bundle is now correctly optional via the new `getBundleDependenciesOrNull` helper (matches existing `getDependencyOrNull`).


## 0.7.4 *(2026-04-25)*
- Apply manifest placeholders to device tests

## 0.7.3 *(2026-04-21)*
- Add `manifestPlaceholders` DSL

## 0.7.2 *(2026-04-18)*
- Add support for UI renderer bindings

## 0.7.1 *(2026-04-17)*
- Introduce a KSP-based navigation codegen that eliminates per-destination DI boilerplate in Metro + Decompose KMP projects.

## 0.6.7 *(2026-04-10)*
- Remove SKIE DSL

## 0.6.6 *(2026-04-09)*
- Fix iOS test crashes for modules using moko resources.
- Copy moko resource bundles to iOS test binaries so localization tests run on iOS.
- Optimize `MokoResourceGeneratorTask`: remove dead incremental logic, reduce duplication, scope compile dependency to `commonMain`.
- Add golden file tests for moko resource code generation.

## 0.6.4 *(2026-04-01)*
- Add Metro Gradle plugin and configure contribution providers.
- Add support for Compose UI tests.
- Dependency updates.

## 0.6.3 *(2026-03-26)*
- Remove Tag publishing from beta builds

## 0.6.2 *(2026-03-25)*
- Add support for beta version bumps.
- Update `AppPlugin` to use `BUILD_NUMBER` and enable release optimizations.

## 0.6.1 *(2026-03-24)*
- Push release tags to origin automatically during release.

## 0.6.0 *(2026-03-24)*
- Add release automation tasks (`bumpVersion`, `release`).
- Dependency updates.

## 0.5.1 *(2026-02-26)*
- Enable new compiler features: explicit backing fields, reified types in catch clauses, and `@all:` annotation target.
- Fix dependency analysis plugin configuration for KMP projects.
- Replace deprecated `-Xjvm-default` compiler flag with `-jvm-default`.
- Dependency updates.


## 0.5.0 *(2026-02-12)*
- Migrate to gradle 9.0
- Delete `AndroidMultiplatformPlugin` plugin. We now only need `KotlinMultiplatformPlugin`
- Remove `org.jetbrains.kotlin.android`
- Delete deprecated androidTarget support.

## 0.4.3 *(2025-12-30)*
- Enable return-value-checker.
- Enable `explicitApi` by default and remove it from base extension.
- Update java-toolchain version to 25.
- Dependency updates.

## 0.4.2
- Rename `useSkies()` to `useSkie()`
- Fix typo in release key alias property (`rReleaseKeyAlias` → `releaseKeyAlias`)
- Make `MokoResourceGeneratorTask` package name configurable via `resourcePackage` property
- Improve configuration cache compatibility and task disabling performance
- Add validation for release signing properties and BuildConfig package name
- Remove duplicate `-Xconsistent-data-class-copy-visibility` compiler argument

## 0.4.1
- Dependency updates.

## 0.4.0
- Add "io.github.thomaskioko.gradle.plugins.buildconfig" plugin. Helps us read keys from `local.properties`

**Usage**
```kotlin

buildConfig {

    packageName.set("com.thomaskioko.tvmaniac.core.base")
    booleanField("IS_DEBUG", true)
    buildConfigField("TMDB_API_KEY")
    buildConfigField("CLIENT_ID")
    buildConfigField("CLIENT_SECRET")
}
```

## 0.3.2
- Added `useSkie()` to DSL
- Group  Kotlin, KSP & Skie in Renovate
- Enable Kotlin/Native binary size optimization (Experimental) - Reduces release binary size
- Simplify DisableTasks and fix library lint configuration


## 0.3.0
- CI/CD automation
- Enable tests in multiplatform by default
- Configure ktlint & spotless.
  - Fix formatting on files

## 0.2.1
- Add debug flag and disable tasks during development.
- Update dependencies.

## 0.1.0

- Initial stable release
- Maven Central publishing support

### Plugin Inventory
- `com.thomaskioko.gradle.app` - Android application modules
- `com.thomaskioko.gradle.android` - Android library modules
- `com.thomaskioko.gradle.android.multiplatform` - Android targets in KMP
- `com.thomaskioko.gradle.jvm` - JVM modules
- `com.thomaskioko.gradle.multiplatform` - Kotlin Multiplatform modules
- `com.thomaskioko.gradle.base` - Base configurations
- `com.thomaskioko.gradle.root` - Root project setup
- `com.thomaskioko.gradle.baseline.profile` - Android baseline profiles
- `com.thomaskioko.gradle.spotless` - Code formatting with Spotless
- `com.thomaskioko.resource.generator` - Moko resource generation

