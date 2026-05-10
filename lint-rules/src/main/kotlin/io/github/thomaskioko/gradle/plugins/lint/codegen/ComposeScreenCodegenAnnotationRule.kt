package io.github.thomaskioko.gradle.plugins.lint.codegen

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import io.github.thomaskioko.gradle.plugins.lint.RULE_ABOUT
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Requires every Compose function that takes a presenter parameter to carry a codegen UI
 * annotation that wires it into the navigation host's renderer multibinding.
 *
 * The rule fires on a top-level `@Composable` function that declares a parameter named
 * `presenter` or `rootPresenter` and is missing every accepted codegen UI annotation
 * (`@ScreenUi`, `@SheetUi`, `@TabUi`, `@AppRootUi`). Without one of those annotations the
 * navigation host cannot dispatch the composable through `Set<ScreenContent>`,
 * `Set<SheetContent>`, or the generated `AppRootProvider`, so the composable is unreachable.
 *
 * Screens dispatched manually inside a parent host (rare; the standard tab path is `@TabUi`) opt
 * out by listing their simple function name in `ktlint_tvmaniac_unrouted_screens` in the project
 * `.editorconfig`.
 *
 * ## Example
 *
 * ```kotlin
 * // Forbidden:
 * @Composable
 * fun TrendingShowsScreen(
 *     presenter: TrendingShowsPresenter,
 *     modifier: Modifier = Modifier,
 * ) { ... }
 *
 * // Allowed:
 * @ScreenUi(presenter = TrendingShowsPresenter::class, parentScope = ActivityScope::class)
 * @Composable
 * fun TrendingShowsScreen(
 *     presenter: TrendingShowsPresenter,
 *     modifier: Modifier = Modifier,
 * ) { ... }
 *
 * // Allowed (host composable):
 * @AppRootUi(presenter = RootPresenter::class, parentScope = ActivityScope::class)
 * @Composable
 * fun RootScreen(
 *     rootPresenter: RootPresenter,
 *     screenContents: Set<ScreenContent>,
 *     sheetContents: Set<SheetContent>,
 *     modifier: Modifier = Modifier,
 * ) { ... }
 * ```
 */
public class ComposeScreenCodegenAnnotationRule :
    Rule(
        ruleId = RuleId("tvmaniac:compose-screen-needs-codegen-annotation"),
        about = RULE_ABOUT,
        usesEditorConfigProperties = setOf(UNROUTED_SCREENS_PROPERTY),
    ),
    RuleAutocorrectApproveHandler {
    private var unroutedScreens: Set<String> = UNROUTED_SCREENS_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        unroutedScreens = editorConfig[UNROUTED_SCREENS_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != FUN) return

        val function = node.psi as? KtNamedFunction ?: return
        if (!function.isTopLevel) return

        val name = function.name ?: return
        if (name in unroutedScreens) return

        val annotationNames = function.annotationEntries
            .mapNotNullTo(mutableSetOf()) { it.shortName?.asString() }
        if (COMPOSABLE_ANNOTATION !in annotationNames) return
        if (!function.hasPresenterParameter()) return
        if (UI_CODEGEN_ANNOTATIONS.any { it in annotationNames }) return

        emit(node.startOffset, errorMessage(name), false)
    }

    private fun KtNamedFunction.hasPresenterParameter(): Boolean =
        valueParameters.any { it.name in PRESENTER_PARAMETER_NAMES }

    public companion object {
        internal const val COMPOSABLE_ANNOTATION: String = "Composable"

        internal val PRESENTER_PARAMETER_NAMES: Set<String> = setOf(
            "presenter",
            "rootPresenter",
        )

        internal val UI_CODEGEN_ANNOTATIONS: Set<String> = setOf(
            "ScreenUi",
            "SheetUi",
            "TabUi",
            "AppRootUi",
        )

        internal fun errorMessage(functionName: String): String =
            "$functionName is a @Composable function that accepts a presenter parameter, but is " +
                "missing a codegen UI annotation. Add @ScreenUi(...) for a stack screen, @SheetUi(...) " +
                "for a modal overlay, @TabUi(...) for a bottom-nav tab pager page, or @AppRootUi(...) " +
                "for the application's host composable. If the screen is dispatched manually inside a " +
                "parent host, add its simple name to `ktlint_tvmaniac_unrouted_screens` in " +
                "`.editorconfig`."
    }
}
