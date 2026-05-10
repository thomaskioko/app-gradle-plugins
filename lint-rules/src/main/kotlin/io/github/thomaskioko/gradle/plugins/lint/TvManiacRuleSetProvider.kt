package io.github.thomaskioko.gradle.plugins.lint

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import io.github.thomaskioko.gradle.plugins.lint.codegen.ComposeScreenCodegenAnnotationRule
import io.github.thomaskioko.gradle.plugins.lint.codegen.PresenterCodegenAnnotationRule
import io.github.thomaskioko.gradle.plugins.lint.metro.MetroRedundantInjectRule
import io.github.thomaskioko.gradle.plugins.lint.navigation.NoCustomNavigatorInterfaceRule
import io.github.thomaskioko.gradle.plugins.lint.navigation.NoMutatingRouterImportRule
import io.github.thomaskioko.gradle.plugins.lint.navigation.NoNavigationConstructOutsideNavRule
import io.github.thomaskioko.gradle.plugins.lint.preview.NoStyleWrapperInPreviewRule
import io.github.thomaskioko.gradle.plugins.lint.tests.TestNameFormatRule

/**
 * Entry point for the Tv Maniac ktlint rule set. Implements [RuleSetProviderV3] so ktlint and
 * Spotless discover the rules through the Java service loader on the classpath.
 *
 * The rule set ID is `tvmaniac`. Each rule reports under `tvmaniac:<rule-id>` in lint output.
 *
 * ## Rules
 *
 * - [NoMutatingRouterImportRule] (`tvmaniac:no-mutating-router-import`) blocks Decompose router
 *   mutation imports outside `navigation/` modules.
 * - [NoNavigationConstructOutsideNavRule] (`tvmaniac:no-navigation-construct-outside-nav`) blocks
 *   constructing `StackNavigation` and `SlotNavigation` outside `navigation/` modules.
 * - [NoCustomNavigatorInterfaceRule] (`tvmaniac:no-custom-navigator-interface`) blocks declaring
 *   feature specific `*Navigator` interfaces; presenters must inject the canonical `Navigator` or
 *   `SheetNavigator` from `navigation/api`.
 * - [NoStyleWrapperInPreviewRule] (`tvmaniac:no-style-wrapper-in-preview`) blocks redundant
 *   styling wrappers inside `@Preview` composables (configurable via `.editorconfig`).
 * - [MetroRedundantInjectRule] (`tvmaniac:metro-redundant-inject`) removes redundant `@Inject`
 *   on classes that already declare a Metro `@Contributes...` annotation.
 * - [PresenterCodegenAnnotationRule] (`tvmaniac:presenter-needs-codegen-annotation`) requires
 *   every Metro-injected `Presenter` class to carry `@NavDestination` or `@AppRoot` so the
 *   codegen processor wires it into the navigation graph.
 * - [ComposeScreenCodegenAnnotationRule] (`tvmaniac:compose-screen-needs-codegen-annotation`)
 *   requires every `@Composable` function with a presenter parameter to carry `@ScreenUi`,
 *   `@SheetUi`, or `@AppRootUi` so the codegen processor wires it into the renderer
 *   multibinding.
 * - [TestNameFormatRule] (`tvmaniac:test-name-format`) enforces the `should X given Y` test
 *   naming convention.
 *
 * Consumers do not reference this class directly. ktlint loads it through the service file at
 * `META-INF/services/com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3`.
 */
@Suppress("unused")
public class TvManiacRuleSetProvider : RuleSetProviderV3(RuleSetId(RULE_SET_ID)) {
    override fun getRuleProviders(): Set<RuleProvider> = setOf(
        RuleProvider { NoMutatingRouterImportRule() },
        RuleProvider { NoNavigationConstructOutsideNavRule() },
        RuleProvider { NoCustomNavigatorInterfaceRule() },
        RuleProvider { NoStyleWrapperInPreviewRule() },
        RuleProvider { MetroRedundantInjectRule() },
        RuleProvider { PresenterCodegenAnnotationRule() },
        RuleProvider { ComposeScreenCodegenAnnotationRule() },
    )

    private companion object {
        const val RULE_SET_ID = "tvmaniac"
    }
}
