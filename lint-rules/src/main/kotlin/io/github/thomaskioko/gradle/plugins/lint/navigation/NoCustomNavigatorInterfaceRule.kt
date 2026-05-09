package io.github.thomaskioko.gradle.plugins.lint.navigation

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import io.github.thomaskioko.gradle.plugins.lint.RULE_ABOUT
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtClass

/**
 * Blocks the introduction of feature specific `*Navigator` interfaces.
 *
 * The Tv Maniac codebase has exactly two canonical navigator types, both declared in
 * `navigation/api`: `Navigator` for stack navigation and `SheetNavigator` for modal overlays.
 * Presenters that need to navigate inject one of these. Adding a feature specific
 * `ShowDetailsNavigator`, `HomeTabNavigator`, or similar fragments the navigation contract
 * across feature modules and makes it harder to reason about the navigation graph as a whole.
 *
 * The rule fires on any `interface` whose name ends in `Navigator` and is not one of
 * [CANONICAL_NAVIGATORS]. Implementation classes that end in `Navigator` (for example a
 * `DefaultNavigator` class) are unaffected; only the interface declaration is.
 *
 * Adding a new canonical navigator requires architecture review, which is what the error
 * message instructs the author to seek.
 *
 * ## Example
 *
 * ```kotlin
 * // Forbidden:
 * interface ShowDetailsNavigator
 *
 * // Allowed:
 * interface Navigator
 * interface SheetNavigator
 * class DefaultNavigator    // implementation, not interface
 * ```
 */
public class NoCustomNavigatorInterfaceRule :
    Rule(
        ruleId = RuleId("tvmaniac:no-custom-navigator-interface"),
        about = RULE_ABOUT,
    ),
    RuleAutocorrectApproveHandler {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != CLASS) return
        val ktClass = node.psi as? KtClass ?: return
        if (!ktClass.isInterface()) return

        val name = ktClass.name ?: return
        if (!name.endsWith(NAVIGATOR_SUFFIX)) return
        if (name in CANONICAL_NAVIGATORS) return

        emit(node.startOffset, errorMessage(name), false)
    }

    public companion object {
        internal const val NAVIGATOR_SUFFIX: String = "Navigator"
        internal val CANONICAL_NAVIGATORS: Set<String> = setOf(
            "Navigator",
            "SheetNavigator",
        )

        /**
         * Builds the lint error message for a forbidden navigator interface.
         *
         * @param name The simple name of the offending interface.
         */
        public fun errorMessage(name: String): String =
            "Custom Navigator interface '$name' is forbidden. " +
                "Use canonical Navigator or SheetNavigator from navigation/api. " +
                "New navigators require an architecture review."
    }
}
