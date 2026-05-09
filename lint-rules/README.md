# Lint Rules

A small ktlint rule set that enforces project specific conventions for the codebase. This is loaded into
Spotless through the lint convention plugin in `plugins/`.
Six rules cover:
- Navigation layering
- Compose preview styling
- Metro Dependency injection cleanup
- Test naming

## Rules

Each rule reports under the `tvmaniac` rule set ID, so rule IDs in lint output look like `tvmaniac:no-mutating-router-import`.

### `tvmaniac:no-mutating-router-import`

Blocks Decompose router mutation imports outside the navigation layer (configurable, see [Configuring the navigation layer](#configuring-the-navigation-layer)). The two read only types (`ChildStack`, `ChildSlot`) that render site presenters and UIs legitimately depend on remain allowed.

Decompose's `router.stack` and `router.slot` packages contain both read only types and mutation primitives (`StackNavigation`, `SlotNavigation`, `pushNew`, `pop`, `activate`, etc.). Mutation belongs inside the navigation layer where the canonical `Navigator` and `SheetNavigator` are implemented. Allowing arbitrary modules to import mutation primitives would let any presenter mutate the back stack directly, bypassing the navigation contract.

```kotlin
// Forbidden in features/show-details/presenter:
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pushNew

// Allowed anywhere:
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.slot.ChildSlot
```

Wildcard imports (for example `com.arkivanov.decompose.router.stack.*`) also fire because a wildcard pulls in the mutation symbols alongside the read only ones.

### `tvmaniac:no-navigation-construct-outside-nav`

Blocks construction of `StackNavigation()` and `SlotNavigation()` outside the navigation layer (configurable, see [Configuring the navigation layer](#configuring-the-navigation-layer)). Type references (parameter types, return types) are unaffected; only the construction call is.

```kotlin
// Forbidden in features/home/presenter:
private val stack = StackNavigation<HomeRoute>()

// Allowed (type reference, not construction):
fun navigate(stack: StackNavigation<HomeRoute>)
```

### `tvmaniac:no-custom-navigator-interface`

Blocks declaring feature specific `*Navigator` interfaces. The codebase has two canonical navigators in `navigation/api`: `Navigator` for stack navigation and `SheetNavigator` for modal overlays. Adding a new canonical navigator requires architecture review.

```kotlin
// Forbidden:
interface ShowDetailsNavigator

// Allowed:
interface Navigator         // canonical
interface SheetNavigator    // canonical
class DefaultNavigator      // implementation; rule only blocks interfaces
```

### `tvmaniac:no-style-wrapper-in-preview`

Blocks redundant styling wrappers inside `@Preview` composables (configurable, see [Configuring preview wrappers](#configuring-preview-wrappers)). Every preview runs inside `TvManiacPreviewWrapperProvider`, which applies the project theme and background once. Wrapping the preview body again applies styling twice, which is redundant and lets individual previews drift from the project wide preview styling when the wrapper provider is updated.

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

The rule fires on any function whose annotation simple name contains `"Preview"`, so it covers `@Preview`, `@PreviewLightDark`, `@ThemePreviews`, and other multi preview annotations.

### `tvmaniac:metro-redundant-inject`

Removes redundant `@Inject` from classes that already declare a Metro `@Contributes...` annotation. Metro applies `@Inject` implicitly when any of `@ContributesBinding`, `@ContributesIntoSet`, `@ContributesIntoMap`, or `@ContributesTo` is present on a class. Adding `@Inject` on top of one of those annotations is duplicate and clutters the declaration.

```kotlin
// Forbidden:
@ContributesBinding(AppScope::class)
@Inject
class FooImpl : Foo

// Allowed (Metro applies @Inject implicitly):
@ContributesBinding(AppScope::class)
class FooImpl : Foo
```

The fix is autocorrect-able. The rule covers both class-level `@Inject` and primary-constructor-level `@Inject` (`@Inject constructor(...)`); either is removed when a `@Contributes...` annotation is present on the class.

### `tvmaniac:test-name-format`

Enforces the BDD style `should X given Y` (or `should X when Y`) test naming convention. Both backticked and camelCase forms are accepted; camelCase is needed for `src/androidTest/` because DEX format 037 forbids spaces in identifiers.

```kotlin
// Allowed:
@Test fun `should emit initial state given no data`() {}
@Test fun shouldRenderHomeScreenGivenAuthenticatedUser() {}

// Forbidden:
@Test fun `initial active root should be Discover`() {}             // missing 'should' prefix
@Test fun `should display correct watch progress percentage`() {}    // missing 'given' or 'when'
```

The rule covers `@Test`, `@ParameterizedTest`, and `@RepeatedTest`. Lifecycle annotations (`@BeforeTest`, `@AfterEach`, etc.) are ignored because they are setup and teardown hooks, not tests.

## Configuring the navigation layer

The `tvmaniac:no-mutating-router-import` and `tvmaniac:no-navigation-construct-outside-nav` rules need to know which modules form the navigation layer. They share a single `.editorconfig` property:

```
[*.{kt,kts}]
ktlint_tvmaniac_navigation_module_paths = navigation
```

- **Default**: `navigation`. Any file under a directory named `navigation/` is treated as part of the navigation layer.
- **Multiple roots**: comma-separated, for example `navigation, routing`. Useful during a rename, or when the navigation layer is split across two top-level groups.
- **Multi-segment entries**: keep slashes, for example `feature/nav`. The entry is matched as `/feature/nav/`.
- **Slash trimming**: leading and trailing slashes are stripped, so `/navigation/` and `navigation` behave identically.
- **Blank entries** are ignored. Setting the property to `unset` (or leaving the value empty) makes both rules treat no path as part of the navigation layer; they then fire everywhere their primary check matches.
- **Case sensitive**: the path match honours the casing on disk.

## Configuring preview wrappers

The `tvmaniac:no-style-wrapper-in-preview` rule looks at two `.editorconfig` properties to decide which calls inside a `@Preview` body count as redundant styling wrappers. Either input alone is enough to trigger a violation; both can be combined.

### `ktlint_tvmaniac_preview_wrappers`

Comma-separated list of simple call names. Each entry is matched as a literal name against the call site.

```
[*.{kt,kts}]
ktlint_tvmaniac_preview_wrappers = TvManiacTheme, TvManiacBackground, Surface, MaterialTheme
```

- **Default**: `TvManiacTheme, TvManiacBackground, Surface, MaterialTheme`. Catches the project design system wrappers and the two generic Material wrappers a developer is most likely to reach for as a substitute.
- **Whitespace** around entries is trimmed; **blank entries** are ignored.
- Setting the property to `unset` (or leaving the value empty) disables simple-name matching. The rule then relies on `ktlint_tvmaniac_preview_wrapper_packages` alone, and fires nowhere if both inputs are empty.

### `ktlint_tvmaniac_preview_wrapper_packages`

Comma-separated list of fully qualified name prefixes. The rule walks the file's `import` directives to resolve each call's simple name to its FQN, then checks whether that FQN equals or starts with any configured prefix.

```
[*.{kt,kts}]
ktlint_tvmaniac_preview_wrapper_packages = com.thomaskioko.tvmaniac.designsystem.theme, androidx.compose.material3.Surface
```

- **Default**: empty.
- Useful as the rename-resilience input. A rename of `TvManiacTheme` to `MyAppTheme` inside `com.thomaskioko.tvmaniac.designsystem.theme` is still caught when that package is in the prefix set, even if the new name is not in `ktlint_tvmaniac_preview_wrappers`.
- Each entry may be a **package name** (such as `com.example.theme`) or a **complete symbol FQN** (such as `androidx.compose.material3.Surface`). The match is exact equality or `prefix.` followed by more characters.
- **Star imports** (for example `import com.example.theme.*`) cannot be resolved without type information, so a call whose binding comes from a star import will not match a package prefix. List the symbol name explicitly via `ktlint_tvmaniac_preview_wrappers` in that case.

## How it loads

ktlint discovers rule sets through the Java service loader. The published `lint-rules` jar contains a service file at `META-INF/services/com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3` pointing at `TvManiacRuleSetProvider`. Spotless picks the jar up when it is on the classpath of the formatter task, which the `lint` convention plugin in `plugins/` wires up automatically for every module that applies a `scaffold {}` plugin.

To enable the rule set in a project that does not use the convention plugins:

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

Four steps:

1. Add a new Kotlin file under `lint-rules/src/main/kotlin/io/github/thomaskioko/gradle/plugins/lint/`. The class extends `Rule` (and typically `RuleAutocorrectApproveHandler`) and carries a `RuleId("tvmaniac:<rule-id>")` plus the shared `RULE_ABOUT` metadata.
2. Add a matching test under `lint-rules/src/test/kotlin/...` using `KtLintAssertThat.assertThatRule { YourRule() }`. Cover both the violations (the rule fires here) and the negative cases (the rule does not fire here).
3. Register the rule in `TvManiacRuleSetProvider.getRuleProviders()`.
4. Run `./gradlew :lint-rules:test` to confirm the new tests pass, and `./gradlew :lint-rules:spotlessCheck` to confirm formatting.

## References

- ktlint: https://pinterest.github.io/ktlint/
- ktlint custom rule sets: https://pinterest.github.io/ktlint/latest/api/custom-rule-set/
- Decompose: https://arkivanov.github.io/Decompose/
- Spotless: https://github.com/diffplug/spotless/tree/main/plugin-gradle
