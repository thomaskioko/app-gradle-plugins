# Lint Rules

This is a small ktlint rule set that enforces the conventions of this codebase. The lint convention plugin in `plugins/` loads it into
Spotless. There are seven rules, and they cover:
- Navigation layering
- Compose preview styling
- Metro Dependency injection cleanup
- Codegen annotation discipline (presenter)
- Test naming

## Rules

Every rule reports under the `tvmaniac` rule set ID, so rule IDs in lint output look like `tvmaniac:no-mutating-router-import`.

### `tvmaniac:no-mutating-router-import`

This rule blocks Decompose router mutation imports outside the navigation layer (configurable, see [Configuring the navigation layer](#configuring-the-navigation-layer)). The two read only types that presenters and UIs render from (`ChildStack`, `ChildSlot`) stay allowed.

Decompose's `router.stack` and `router.slot` packages hold both read only types and mutation primitives (`StackNavigation`, `SlotNavigation`, `pushNew`, `pop`, `activate`, etc.). We keep mutation inside the navigation layer, where the canonical `Navigator` and `SheetNavigator` live. If any module could import the mutation primitives, any presenter could change the back stack directly and skip the navigation contract.

```kotlin
// Forbidden in features/show-details/presenter:
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pushNew

// Allowed anywhere:
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.slot.ChildSlot
```

Wildcard imports (for example `com.arkivanov.decompose.router.stack.*`) also fire. A wildcard pulls in the mutation symbols along with the read only ones.

### `tvmaniac:no-navigation-construct-outside-nav`

This rule blocks constructing `StackNavigation()` and `SlotNavigation()` outside the navigation layer (configurable, see [Configuring the navigation layer](#configuring-the-navigation-layer)). Type references (parameter types, return types) are fine. Only the construction call fires.

```kotlin
// Forbidden in features/home/presenter:
private val stack = StackNavigation<HomeRoute>()

// Allowed (type reference, not construction):
fun navigate(stack: StackNavigation<HomeRoute>)
```

### `tvmaniac:no-custom-navigator-interface`

This rule blocks feature specific `*Navigator` interfaces. The codebase has two canonical navigators in `navigation/api`: `Navigator` for stack navigation and `SheetNavigator` for modal overlays. Adding a third one needs an architecture review.

```kotlin
// Forbidden:
interface ShowDetailsNavigator

// Allowed:
interface Navigator         // canonical
interface SheetNavigator    // canonical
class DefaultNavigator      // implementation; rule only blocks interfaces
```

### `tvmaniac:no-style-wrapper-in-preview`

This rule blocks extra styling wrappers inside `@Preview` composables (configurable, see [Configuring preview wrappers](#configuring-preview-wrappers)). Every preview already runs inside `TvManiacPreviewWrapperProvider`, which applies the project theme and background once. Wrapping the preview body again applies the styling twice. It also lets a single preview drift from the shared preview styling when someone updates the wrapper provider.

```kotlin
// Forbidden:
@Preview
@Composable
private fun DiscoverScreenPreview() {
    TvManiacTheme {
        DiscoverScreen()
    }
}

// Allowed:
@Preview
@PreviewWrapper(TvManiacPreviewWrapperProvider::class)
@Composable
private fun DiscoverScreenPreview() {
    DiscoverScreen()
}
```

The rule fires on any function with an annotation whose simple name contains `"Preview"`. That covers `@Preview`, `@PreviewLightDark`, `@ThemePreviews`, and other multi preview annotations.

### `tvmaniac:metro-redundant-inject`

This rule removes `@Inject` from classes that already carry a Metro `@Contributes...` annotation. Metro applies `@Inject` implicitly when a class has `@ContributesBinding`, `@ContributesIntoSet`, `@ContributesIntoMap`, or `@ContributesTo`. Adding `@Inject` on top is a duplicate and just clutters the declaration.

```kotlin
// Forbidden:
@ContributesBinding(AppScope::class)
@Inject
class FooImpl : Foo

// Allowed (Metro applies @Inject implicitly):
@ContributesBinding(AppScope::class)
class FooImpl : Foo
```

The rule can autocorrect. It handles both `@Inject` on the class and `@Inject` on the primary constructor (`@Inject constructor(...)`). Either one is removed when the class has a `@Contributes...` annotation.

### `tvmaniac:presenter-needs-codegen-annotation`

This rule requires every Metro injected presenter class to also carry a codegen annotation that wires it into navigation. It fires on any class that matches all three of these: its simple name ends with `Presenter`, it has `@Inject` or `@AssistedInject`, and it has none of the accepted codegen annotations (`@NavDestination`, `@AppRoot`, `@ChildPresenter`).

```kotlin
// Forbidden:
@Inject
class TrendingShowsPresenter(...)

// Allowed:
@Inject
@NavDestination(
    route = TrendingShowsRoute::class,
    parentScope = ActivityScope::class,
    kind = DestinationKind.SCREEN,
)
class TrendingShowsPresenter(...)

// Allowed (root presenter):
@AppRoot(parentScope = ActivityScope::class)
@AssistedInject
class DefaultRootPresenter(...) : RootPresenter

// Allowed (child presenter owned by a parent):
@Inject
@ChildPresenter(scope = UpNextScope::class, parentScope = ActivityScope::class)
class UpNextPresenter(...)
```

Classes with `@ContributesBinding`, `@ContributesIntoSet`, or `@ContributesIntoMap` are exempt, because Metro wires them through the binding and not through codegen. Abstract and interface presenters are exempt for the same reason. Child presenters annotated with `@ChildPresenter` pass the rule directly. Presenters exposed through a hand written `@GraphExtension` opt out by listing their simple class name in `ktlint_tvmaniac_unrouted_presenters`. See [Configuring codegen exemptions](#configuring-codegen-exemptions).

Some composables render a presenter without being navigation destinations. Examples are a host that places child component composables directly, or an embedded reusable component. These are plain composables and carry no UI annotation. We deliberately have no rule that requires one. A ktlint rule cannot resolve the presenter's type, so it cannot tell an embedded child UI from a routed screen. Codegen only wires annotated presenters (`@NavDestination`, `@AppRoot`, `@ChildPresenter`) and the `@ScreenUi`/`@SheetUi`/`@TabUi`/`@AppRootUi` composables.

### `tvmaniac:test-name-format`

This rule enforces the BDD style test names `should X given Y` (or `should X when Y`). It accepts both backticked and camelCase names. You need camelCase in `src/androidTest/`, because DEX format 037 does not allow spaces in identifiers.

```kotlin
// Allowed:
@Test fun `should emit initial state given no data`() {}
@Test fun shouldRenderHomeScreenGivenAuthenticatedUser() {}

// Forbidden:
@Test fun `initial active root should be Discover`() {}             // missing 'should' prefix
@Test fun `should display correct watch progress percentage`() {}    // missing 'given' or 'when'
```

The rule checks `@Test`, `@ParameterizedTest`, and `@RepeatedTest`. It ignores lifecycle annotations (`@BeforeTest`, `@AfterEach`, etc.), since those mark setup and teardown hooks, not tests.

## Configuring codegen exemptions

The `tvmaniac:presenter-needs-codegen-annotation` rule reads one `.editorconfig` property. It lists the presenters that opt out of the requirement.

### `ktlint_tvmaniac_unrouted_presenters`

This is a comma separated list of simple class names for presenters that are deliberately not wired through codegen. Use it for presenters exposed through a hand written `@GraphExtension`. A child presenter that the codegen should wire takes `@ChildPresenter` instead.

```
[*.{kt,kts}]
ktlint_tvmaniac_unrouted_presenters = UpNextPresenter, CalendarPresenter
```

- **Default**: empty. Every Metro injected `Presenter` class must carry `@NavDestination`, `@AppRoot`, or `@ChildPresenter`.
- **Matching** ignores case.
- **Whitespace** around entries is trimmed; **blank entries** are ignored.
- Setting the property to `unset` (or leaving the value empty) keeps the default empty list.

## Configuring the navigation layer

The `tvmaniac:no-mutating-router-import` and `tvmaniac:no-navigation-construct-outside-nav` rules need to know which modules make up the navigation layer. Both read the same `.editorconfig` property:

```
[*.{kt,kts}]
ktlint_tvmaniac_navigation_module_paths = navigation
```

- **Default**: `navigation`. Any file under a directory named `navigation/` counts as part of the navigation layer.
- **Multiple roots**: comma separated, for example `navigation, routing`. This helps during a rename, or when the navigation layer is split across two top level groups.
- **Multi-segment entries**: keep the slashes, for example `feature/nav`. The entry is matched as `/feature/nav/`.
- **Slash trimming**: leading and trailing slashes are stripped, so `/navigation/` and `navigation` behave the same.
- **Blank entries** are ignored. If you set the property to `unset` (or leave the value empty), no path counts as the navigation layer. Both rules then fire everywhere their main check matches.
- **Case sensitive**: the path match follows the casing on disk.

## Configuring preview wrappers

The `tvmaniac:no-style-wrapper-in-preview` rule reads two `.editorconfig` properties to decide which calls inside a `@Preview` body count as extra styling wrappers. Either one alone is enough to trigger a violation, and you can combine them.

### `ktlint_tvmaniac_preview_wrappers`

This is a comma separated list of simple call names. Each entry is matched as a literal name against the call site.

```
[*.{kt,kts}]
ktlint_tvmaniac_preview_wrappers = TvManiacTheme, TvManiacBackground, Surface, MaterialTheme
```

- **Default**: `TvManiacTheme, TvManiacBackground, Surface, MaterialTheme`. This catches the project's design system wrappers, plus the two generic Material wrappers a developer is most likely to use instead.
- **Whitespace** around entries is trimmed; **blank entries** are ignored.
- Setting the property to `unset` (or leaving the value empty) turns off simple name matching. The rule then relies only on `ktlint_tvmaniac_preview_wrapper_packages`, and fires nowhere if both are empty.

### `ktlint_tvmaniac_preview_wrapper_packages`

This is a comma separated list of fully qualified name prefixes. The rule reads the file's `import` directives to resolve each call's simple name to its FQN. It then checks whether that FQN equals, or starts with, any configured prefix.

```
[*.{kt,kts}]
ktlint_tvmaniac_preview_wrapper_packages = com.thomaskioko.tvmaniac.designsystem.theme, androidx.compose.material3.Surface
```

- **Default**: empty.
- Use this one to survive renames. Say someone renames `TvManiacTheme` to `MyAppTheme` inside `com.thomaskioko.tvmaniac.designsystem.theme`. If that package is in the prefix set, the rule still catches it, even though the new name is not in `ktlint_tvmaniac_preview_wrappers`.
- Each entry can be a **package name** (such as `com.example.theme`) or a **complete symbol FQN** (such as `androidx.compose.material3.Surface`). A match is either exact equality or `prefix.` followed by more characters.
- **Star imports** (for example `import com.example.theme.*`) cannot be resolved without type information. A call that comes from a star import will not match a package prefix. In that case, list the symbol name in `ktlint_tvmaniac_preview_wrappers`.

## How it loads

ktlint finds rule sets through the Java service loader. The published `lint-rules` jar has a service file at `META-INF/services/com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3` that points at `TvManiacRuleSetProvider`. Spotless picks up the jar when it is on the formatter task's classpath. The `lint` convention plugin in `plugins/` puts it there for every module that applies a `scaffold {}` plugin.

To turn on the rule set in a project that does not use the convention plugins:

```kotlin
// build.gradle.kts
spotless {
    kotlin {
        ktlint("1.5.0").customRuleSets(
            listOf("io.github.thomaskioko.gradle.plugins:lint-rules:<version>"),
        )
    }
}
```

## Adding a rule

Adding a rule takes four steps:

1. Add a Kotlin file under `lint-rules/src/main/kotlin/io/github/thomaskioko/gradle/plugins/lint/`. The class extends `Rule` (and usually `RuleAutocorrectApproveHandler`). It carries a `RuleId("tvmaniac:<rule-id>")` and the shared `RULE_ABOUT` metadata.
2. Add a matching test under `lint-rules/src/test/kotlin/...` using `KtLintAssertThat.assertThatRule { YourRule() }`. Cover both the cases where the rule fires and the cases where it does not.
3. Register the rule in `TvManiacRuleSetProvider.getRuleProviders()`.
4. Run `./gradlew :lint-rules:test` to check the new tests pass, and `./gradlew :lint-rules:spotlessCheck` to check formatting.

## References

- ktlint: https://pinterest.github.io/ktlint/
- ktlint custom rule sets: https://pinterest.github.io/ktlint/latest/api/custom-rule-set/
- Decompose: https://arkivanov.github.io/Decompose/
- Spotless: https://github.com/diffplug/spotless/tree/main/plugin-gradle
