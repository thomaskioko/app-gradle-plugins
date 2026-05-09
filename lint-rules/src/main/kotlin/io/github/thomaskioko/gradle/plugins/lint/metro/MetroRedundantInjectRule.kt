package io.github.thomaskioko.gradle.plugins.lint.metro

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import io.github.thomaskioko.gradle.plugins.lint.RULE_ABOUT
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass

/**
 * Removes redundant `@Inject` from classes that already declare a Metro `@Contributes...`
 * annotation.
 *
 * Metro applies `@Inject` implicitly when any of `@ContributesBinding`, `@ContributesIntoSet`,
 * `@ContributesIntoMap`, or `@ContributesTo` is present on a class. Adding `@Inject` on top of
 * one of those annotations is duplicate and clutters the declaration.
 *
 * The rule fires on either of:
 *
 * - A class-level `@Inject` next to a class-level `@Contributes...` annotation.
 * - A primary-constructor-level `@Inject` (`@Inject constructor(...)`) next to a class-level
 *   `@Contributes...` annotation.
 *
 * The fix is autocorrect-able. The redundant `@Inject` is removed along with the surrounding
 * whitespace so the formatted output stays clean.
 *
 * ## Example
 *
 * ```kotlin
 * // Forbidden:
 * @ContributesBinding(AppScope::class)
 * @Inject
 * class FooImpl : Foo
 *
 * // Allowed (Metro applies @Inject implicitly):
 * @ContributesBinding(AppScope::class)
 * class FooImpl : Foo
 * ```
 */
public class MetroRedundantInjectRule :
    Rule(
        ruleId = RuleId("tvmaniac:metro-redundant-inject"),
        about = RULE_ABOUT,
    ),
    RuleAutocorrectApproveHandler {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != CLASS) return

        val ktClass = node.psi as? KtClass ?: return
        if (ktClass.annotationEntries.none { it.isContributes() }) return

        val redundantInject = ktClass.annotationEntries.firstOrNull { it.isInject() }
            ?: ktClass.primaryConstructor?.annotationEntries?.firstOrNull { it.isInject() }
            ?: return

        emit(redundantInject.node.startOffset, ERROR_MESSAGE, true)
            .ifAutocorrectAllowed { redundantInject.removeFromTree() }
    }

    private fun KtAnnotationEntry.removeFromTree() {
        val annotationNode = node
        val modifierList = annotationNode.treeParent

        if (modifierList.firstChildNode === annotationNode && modifierList.lastChildNode === annotationNode) {
            val outerParent = modifierList.treeParent
            val trailingWs = modifierList.treeNext?.takeIf { it.elementType == WHITE_SPACE }
            outerParent.removeChild(modifierList)
            trailingWs?.let(outerParent::removeChild)
            return
        }

        val next = annotationNode.treeNext
        val prev = annotationNode.treePrev
        val whitespaceToRemove = next?.takeIf { it.isWhiteSpaceWithNewline() }
            ?: prev?.takeIf { it.elementType == WHITE_SPACE }
            ?: next?.takeIf { it.elementType == WHITE_SPACE }
        whitespaceToRemove?.let(modifierList::removeChild)
        modifierList.removeChild(annotationNode)
    }

    private fun KtAnnotationEntry.isContributes(): Boolean =
        shortName?.asString() in CONTRIBUTES_ANNOTATIONS

    private fun KtAnnotationEntry.isInject(): Boolean =
        shortName?.asString() == INJECT_ANNOTATION

    private fun ASTNode.isWhiteSpaceWithNewline(): Boolean =
        elementType == WHITE_SPACE && textContains('\n')

    public companion object {
        internal const val INJECT_ANNOTATION: String = "Inject"

        internal val CONTRIBUTES_ANNOTATIONS: Set<String> = setOf(
            "ContributesBinding",
            "ContributesIntoSet",
            "ContributesIntoMap",
            "ContributesTo",
        )

        public const val ERROR_MESSAGE: String =
            "@Inject is redundant when a @Contributes... annotation is present. " +
                "Metro applies @Inject implicitly. Remove the @Inject annotation."
    }
}
