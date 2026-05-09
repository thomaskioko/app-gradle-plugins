package io.github.thomaskioko.gradle.plugins.lint.navigation

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import io.github.thomaskioko.gradle.plugins.lint.RULE_ABOUT
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtFile

/**
 * Blocks Decompose router mutation imports outside the navigation layer. The two read only types
 * (`ChildStack`, `ChildSlot`) that render site presenters and UIs legitimately depend on remain
 * allowed.
 *
 * Decompose's `router.stack` and `router.slot` packages contain both read only types
 * (`ChildStack`, `ChildSlot`) and mutation primitives (`StackNavigation`, `SlotNavigation`,
 * `pushNew`, `pop`, `activate`, etc.). Mutation belongs inside the navigation layer where the
 * canonical `Navigator` and `SheetNavigator` are implemented. Allowing arbitrary modules to
 * import mutation primitives would let any presenter mutate the back stack directly, bypassing
 * the navigation contract.
 *
 * The rule pairs a forbidden prefix list ([FORBIDDEN_PREFIXES]) with an allow list of specific
 * fully qualified names ([ALLOWED_FQNS]). Any import matching a forbidden prefix and not in the
 * allow list fires, unless the file lives inside a module declared by the
 * [NAVIGATION_MODULE_PATHS_PROPERTY] `.editorconfig` property (default: `navigation`).
 *
 * Wildcard imports (for example `com.arkivanov.decompose.router.stack.*`) match the forbidden
 * prefix and fail the allow list, so they fire too. This is the desired behaviour because a
 * wildcard pulls in mutation symbols alongside the read only ones.
 *
 * ## Example
 *
 * ```kotlin
 * // Forbidden in features/show-details/presenter:
 * import com.arkivanov.decompose.router.stack.StackNavigation
 * import com.arkivanov.decompose.router.stack.pushNew
 * import com.arkivanov.decompose.router.stack.*
 *
 * // Allowed anywhere:
 * import com.arkivanov.decompose.router.stack.ChildStack
 * import com.arkivanov.decompose.router.slot.ChildSlot
 *
 * // Allowed only inside modules matched by ktlint_tvmaniac_navigation_module_paths:
 * import com.arkivanov.decompose.router.stack.StackNavigation
 * ```
 */
public class NoMutatingRouterImportRule :
    Rule(
        ruleId = RuleId("tvmaniac:no-mutating-router-import"),
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
        if (node.elementType != IMPORT_DIRECTIVE) return

        val fqn = node.text
            .removePrefix("import ")
            .substringBefore(" as ")
            .trim()

        val isForbiddenPrefix = FORBIDDEN_PREFIXES.any { fqn.startsWith("$it.") }
        if (!isForbiddenPrefix) return
        if (fqn in ALLOWED_FQNS) return

        val ktFile = node.psi.containingFile as? KtFile ?: return
        if (ktFile.virtualFilePath.isInNavigationModule(navigationModulePaths)) return

        emit(node.startOffset, errorMessage(fqn), false)
    }

    public companion object {
        internal val FORBIDDEN_PREFIXES: Set<String> = setOf(
            "com.arkivanov.decompose.router.stack",
            "com.arkivanov.decompose.router.slot",
        )

        internal val ALLOWED_FQNS: Set<String> = setOf(
            "com.arkivanov.decompose.router.stack.ChildStack",
            "com.arkivanov.decompose.router.slot.ChildSlot",
        )

        /**
         * Builds the lint error message for a forbidden router import.
         *
         * @param fqn The fully qualified name of the offending import (or the wildcard form).
         */
        public fun errorMessage(fqn: String): String =
            "Import '$fqn' is forbidden outside navigation modules. " +
                "Use Navigator or SheetNavigator from navigation/api for navigation operations. " +
                "Only ChildStack and ChildSlot may be imported elsewhere (read-only types for render sites)."
    }
}
