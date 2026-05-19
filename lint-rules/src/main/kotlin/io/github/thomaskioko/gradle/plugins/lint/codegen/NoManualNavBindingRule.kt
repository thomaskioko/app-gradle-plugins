package io.github.thomaskioko.gradle.plugins.lint.codegen

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import io.github.thomaskioko.gradle.plugins.lint.RULE_ABOUT
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType

/**
 * Forbids hand-written `@Provides @IntoSet` providers for navigation types that the codegen
 * processor already emits.
 *
 * The codegen module generates a `<Feature>NavDestinationBinding` or `<Feature>TabDestinationBinding`
 * container for every presenter annotated with `@NavDestination` or `@AppRoot`. These containers
 * own the `@IntoSet` contributions for `Set<NavRoot>`, `Set<NavRootBinding<*>>`,
 * `Set<NavDestination<*>>`, and `Set<NavRouteBinding<*>>`. Any hand-written `@Provides @IntoSet`
 * returning one of those four types duplicates the generated provider and creates Metro multibinding
 * conflicts or silent drift when the codegen surface changes.
 *
 * The rule fires on any function declaration that carries both `@Provides` and `@IntoSet` (in any
 * order) and whose declared return type's short name matches one of the four nav types. The fix is
 * to delete the manual provider (and usually the wrapper `@ContributesTo` interface that contains
 * it) and add `@NavDestination` or `@AppRoot` to the sibling presenter if it is missing.
 *
 * ## Example
 *
 * ```kotlin
 * // Forbidden:
 * @ContributesTo(ActivityScope::class)
 * interface ProgressRootBinding {
 *     companion object {
 *         @Provides
 *         @IntoSet
 *         fun provideProgressRoot(): NavRoot = ProgressRoot
 *     }
 * }
 *
 * // Allowed: annotate the presenter and let the processor emit the binding.
 * @NavDestination(
 *     route = ProgressRoute::class,
 *     parentScope = ActivityScope::class,
 *     kind = DestinationKind.TAB_ROOT,
 * )
 * @Inject
 * class ProgressPresenter(...)
 * ```
 */
public class NoManualNavBindingRule :
    Rule(
        ruleId = RuleId("tvmaniac:no-manual-nav-binding"),
        about = RULE_ABOUT,
    ),
    RuleAutocorrectApproveHandler {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != FUN) return

        val ktFunction = node.psi as? KtNamedFunction ?: return

        val annotationNames = ktFunction.annotationEntries
            .mapNotNullTo(mutableSetOf()) { it.shortName?.asString() }
        if (PROVIDES_ANNOTATION !in annotationNames) return
        if (INTO_SET_ANNOTATION !in annotationNames) return

        val returnTypeShortName = ktFunction.typeReference?.shortName() ?: return
        if (returnTypeShortName !in FORBIDDEN_RETURN_TYPES) return

        val functionName = ktFunction.name ?: "<unnamed>"
        emit(node.startOffset, errorMessage(returnTypeShortName, functionName), false)
    }

    private fun KtTypeReference.shortName(): String? {
        val element = typeElement ?: return null
        val userType = when (element) {
            is KtUserType -> element
            is KtNullableType -> element.innerType as? KtUserType
            else -> null
        } ?: return null
        return userType.referencedName
    }

    public companion object {
        internal const val PROVIDES_ANNOTATION: String = "Provides"
        internal const val INTO_SET_ANNOTATION: String = "IntoSet"

        internal val FORBIDDEN_RETURN_TYPES: Set<String> = setOf(
            "NavRoot",
            "NavRootBinding",
            "NavDestination",
            "NavRouteBinding",
        )

        internal fun errorMessage(returnTypeShortName: String, functionName: String): String =
            "`$functionName` provides `$returnTypeShortName` into a multibinding that the codegen " +
                "processor already populates. Delete this manual `@Provides @IntoSet` provider (and " +
                "its wrapper `@ContributesTo` interface if it becomes empty). If the sibling presenter " +
                "is missing `@NavDestination` or `@AppRoot`, add the codegen annotation instead of " +
                "hand-writing the binding."
    }
}
