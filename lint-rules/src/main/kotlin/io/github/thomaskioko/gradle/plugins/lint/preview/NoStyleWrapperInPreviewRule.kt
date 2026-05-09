package io.github.thomaskioko.gradle.plugins.lint.preview

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import io.github.thomaskioko.gradle.plugins.lint.RULE_ABOUT
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Blocks redundant styling wrappers inside `@Preview` family composables.
 *
 * Every preview in the Tv Maniac codebase runs inside `TvManiacPreviewWrapperProvider`, which
 * applies the project theme and background once. Wrapping a preview body in a styling wrapper
 * (such as `TvManiacTheme`, `TvManiacBackground`, `Surface`, or `MaterialTheme`) again applies
 * styling twice, which is redundant and lets individual previews drift from the project wide
 * preview styling when the wrapper provider is updated.
 *
 * Two `.editorconfig` properties decide which calls count as a styling wrapper. Both default to
 * sensible values so the rule is useful out of the box.
 *
 * - [PREVIEW_WRAPPERS_PROPERTY] (`ktlint_tvmaniac_preview_wrappers`) holds simple call names.
 *   Default: `TvManiacTheme, TvManiacBackground, Surface, MaterialTheme`.
 * - [PREVIEW_WRAPPER_PACKAGES_PROPERTY] (`ktlint_tvmaniac_preview_wrapper_packages`) holds fully
 *   qualified name prefixes resolved through the file's `import` list. Default: empty. Useful
 *   when a wrapper has been renamed but still lives inside a known design system package: list
 *   the package and the rule keeps firing without a name update on every rename.
 *
 * The rule fires on any function whose annotation simple name contains `"Preview"`. This catches
 * standard `@Preview`, `@PreviewLightDark`, `@PreviewFontScale`, `@PreviewScreenSizes`,
 * `@PreviewDynamicColors`, plus project defined multi preview annotations such as
 * `@ThemePreviews`. The rule does not fire on Roborazzi screenshot tests or any non preview
 * function. Those legitimately set up the wrapper themselves.
 *
 * ## Example
 *
 * ```kotlin
 * // Forbidden:
 * @Preview
 * @Composable
 * private fun DiscoverScreenPreview() {
 *     TvManiacTheme {
 *         DiscoverScreen()
 *     }
 * }
 *
 * // Allowed (the wrapper provider applies the theme automatically):
 * @Preview
 * @PreviewWrapper(TvManiacPreviewWrapperProvider::class)
 * @Composable
 * private fun DiscoverScreenPreview() {
 *     DiscoverScreen()
 * }
 * ```
 */
public class NoStyleWrapperInPreviewRule :
    Rule(
        ruleId = RuleId("tvmaniac:no-style-wrapper-in-preview"),
        about = RULE_ABOUT,
        usesEditorConfigProperties = setOf(
            PREVIEW_WRAPPERS_PROPERTY,
            PREVIEW_WRAPPER_PACKAGES_PROPERTY,
        ),
    ),
    RuleAutocorrectApproveHandler {
    private var wrappers: Set<String> = PREVIEW_WRAPPERS_PROPERTY.defaultValue
    private var wrapperPackages: Set<String> = PREVIEW_WRAPPER_PACKAGES_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        wrappers = editorConfig[PREVIEW_WRAPPERS_PROPERTY]
        wrapperPackages = editorConfig[PREVIEW_WRAPPER_PACKAGES_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != CALL_EXPRESSION) return

        val reference = node.findChildByType(REFERENCE_EXPRESSION) ?: return
        val callName = reference.text

        if (!callName.matchesWrapper(node)) return

        val enclosingFun = node.psi.findEnclosingPreviewFunction() ?: return

        emit(node.startOffset, errorMessage(callName, enclosingFun.name ?: "<anonymous>"), false)
    }

    private fun String.matchesWrapper(node: ASTNode): Boolean {
        if (this in wrappers) return true
        if (wrapperPackages.isEmpty()) return false
        val ktFile = node.psi.containingFile as? KtFile ?: return false
        val fqn = ktFile.fqnFor(this) ?: return false
        return fqn.startsWithAnyFqn(wrapperPackages)
    }

    private fun PsiElement.findEnclosingPreviewFunction(): KtNamedFunction? {
        var current: PsiElement? = parent
        while (current != null) {
            if (current is KtNamedFunction && current.hasPreviewAnnotation()) return current
            current = current.parent
        }
        return null
    }

    private fun KtNamedFunction.hasPreviewAnnotation(): Boolean =
        annotationEntries.any { entry ->
            entry.shortName?.asString()?.contains(PREVIEW_MARKER) == true
        }

    public companion object {
        internal const val PREVIEW_MARKER: String = "Preview"

        /**
         * Builds the lint error message for a forbidden styling wrapper inside a preview.
         *
         * @param wrapper The simple call name of the offending wrapper composable, taken
         *   verbatim from the source.
         * @param functionName The name of the enclosing preview function the wrapper was found
         *   in.
         */
        public fun errorMessage(wrapper: String, functionName: String): String =
            "Preview '$functionName' wraps its body in '$wrapper { ... }', but the styling is " +
                "applied automatically by TvManiacPreviewWrapperProvider. " +
                "Remove the wrapper and add @PreviewWrapper(TvManiacPreviewWrapperProvider::class) to the preview."
    }
}
