package io.github.thomaskioko.gradle.plugins.lint.navigation

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import io.github.thomaskioko.gradle.plugins.lint.RULE_ABOUT
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtFile

/**
 * Blocks construction of Decompose's `StackNavigation()` and `SlotNavigation()` outside the
 * navigation layer.
 *
 * `StackNavigation` and `SlotNavigation` own the back stack and the overlay slot state
 * respectively. They belong inside the navigation layer where the canonical `Navigator` and
 * `SheetNavigator` are implemented. Constructing them in a feature presenter or a core helper
 * bypasses the navigation contract and gives that code direct mutation power over the back stack.
 *
 * The rule fires on any call expression whose target reference is `StackNavigation` or
 * `SlotNavigation`. Type references (parameter types, return types) are unaffected; only the
 * construction call is. The check skips any file inside a module declared by the
 * [NAVIGATION_MODULE_PATHS_PROPERTY] `.editorconfig` property (default: `navigation`).
 *
 * ## Example
 *
 * ```kotlin
 * // Forbidden in features/home/presenter:
 * private val stack = StackNavigation<HomeRoute>()
 *
 * // Allowed inside modules matched by ktlint_tvmaniac_navigation_module_paths:
 * class DefaultNavigator {
 *     private val stack = StackNavigation<HomeRoute>()
 * }
 *
 * // Allowed anywhere (type reference, not construction):
 * fun navigate(stack: StackNavigation<HomeRoute>)
 * ```
 */
public class NoNavigationConstructOutsideNavRule :
    Rule(
        ruleId = RuleId("tvmaniac:no-navigation-construct-outside-nav"),
        about = RULE_ABOUT,
        usesEditorConfigProperties = setOf(NAVIGATION_MODULE_PATHS_PROPERTY),
    ),
    RuleAutocorrectApproveHandler {
    private var navigationModulePaths: Set<String> = NAVIGATION_MODULE_PATHS_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        navigationModulePaths = editorConfig[NAVIGATION_MODULE_PATHS_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != CALL_EXPRESSION) return

        val reference = node.findChildByType(REFERENCE_EXPRESSION) ?: return
        val name = reference.text
        if (name != STACK_NAVIGATION && name != SLOT_NAVIGATION) return

        val ktFile = node.psi.containingFile as? KtFile ?: return
        if (ktFile.virtualFilePath.isInNavigationModule(navigationModulePaths)) return

        emit(node.startOffset, errorMessage(name), false)
    }

    public companion object {
        internal const val STACK_NAVIGATION: String = "StackNavigation"
        internal const val SLOT_NAVIGATION: String = "SlotNavigation"

        /**
         * Builds the lint error message for forbidden navigation construction.
         *
         * @param name Either `"StackNavigation"` or `"SlotNavigation"`, whichever was
         *   constructed.
         */
        public fun errorMessage(name: String): String =
            "$name<T>() may only be constructed inside a navigation/* module. " +
                "Inject Navigator from navigation/api instead."
    }
}
