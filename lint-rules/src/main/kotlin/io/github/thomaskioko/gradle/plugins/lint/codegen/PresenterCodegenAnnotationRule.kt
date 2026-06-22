package io.github.thomaskioko.gradle.plugins.lint.codegen

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import io.github.thomaskioko.gradle.plugins.lint.RULE_ABOUT
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.lexer.KtTokens

/**
 * Requires every presenter class injected by Metro to also carry a codegen annotation that wires
 * it into the navigation system.
 *
 * The rule fires on a top-level class whose simple name ends with `Presenter`, that is annotated
 * with `@Inject` or `@AssistedInject`, and that is missing every accepted codegen annotation
 * (`@NavDestination`, `@AppRoot`, `@ChildPresenter`). Without one of those annotations the consumer
 * has to write the matching `@GraphExtension` and binding container by hand, defeating the codegen.
 *
 * Classes annotated with `@ContributesBinding` are exempt because Metro wires them through the
 * binding rather than through codegen. Abstract and interface presenters are exempt for the same
 * reason. Child presenters that are exposed through a manual `@GraphExtension` (for example
 * pager children inside a parent presenter) opt out by listing their simple class name in
 * `ktlint_tvmaniac_unrouted_presenters` in the project `.editorconfig`.
 *
 * ## Example
 *
 * ```kotlin
 * // Forbidden:
 * @Inject
 * class TrendingShowsPresenter(...)
 *
 * // Allowed:
 * @Inject
 * @NavDestination(
 *     route = TrendingShowsRoute::class,
 *     parentScope = ActivityScope::class,
 *     kind = DestinationKind.SCREEN,
 * )
 * class TrendingShowsPresenter(...)
 *
 * // Allowed (root presenter):
 * @AppRoot(parentScope = ActivityScope::class)
 * @AssistedInject
 * class DefaultRootPresenter(...) : RootPresenter
 *
 * // Allowed (child presenter on the project's exemption list):
 * @Inject
 * class UpNextPresenter(...)
 * ```
 */
public class PresenterCodegenAnnotationRule :
    Rule(
        ruleId = RuleId("tvmaniac:presenter-needs-codegen-annotation"),
        about = RULE_ABOUT,
        usesEditorConfigProperties = setOf(UNROUTED_PRESENTERS_PROPERTY),
    ),
    RuleAutocorrectApproveHandler {
    private var unroutedPresenters: Set<String> = UNROUTED_PRESENTERS_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        unroutedPresenters = editorConfig[UNROUTED_PRESENTERS_PROPERTY].mapTo(mutableSetOf()) { it.lowercase() }
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != CLASS) return

        val ktClass = node.psi as? KtClass ?: return
        val name = ktClass.name ?: return
        if (!name.endsWith("Presenter")) return
        if (ktClass.isInterface()) return
        if (ktClass.hasModifier(KtTokens.ABSTRACT_KEYWORD)) return
        if (name.lowercase() in unroutedPresenters) return

        val annotationNames = ktClass.annotationEntries
            .mapNotNullTo(mutableSetOf()) { it.shortName?.asString() }
        val hasInjection = INJECTION_ANNOTATIONS.any { it in annotationNames }
        if (!hasInjection) return

        if (BINDING_OPT_OUT_ANNOTATIONS.any { it in annotationNames }) return
        if (CODEGEN_ANNOTATIONS.any { it in annotationNames }) return

        emit(node.startOffset, errorMessage(name), false)
    }

    public companion object {
        internal val INJECTION_ANNOTATIONS: Set<String> = setOf(
            "Inject",
            "AssistedInject",
        )

        internal val CODEGEN_ANNOTATIONS: Set<String> = setOf(
            "NavDestination",
            "NavDestinationAnno",
            "AppRoot",
            "ChildPresenter",
        )

        internal val BINDING_OPT_OUT_ANNOTATIONS: Set<String> = setOf(
            "ContributesBinding",
            "ContributesIntoSet",
            "ContributesIntoMap",
        )

        internal fun errorMessage(className: String): String =
            "$className is annotated with @Inject (or @AssistedInject) and ends with `Presenter`, " +
                "but is missing a codegen annotation. Add @NavDestination(...) for a routed presenter, " +
                "@AppRoot(...) for the application's root presenter, or @ChildPresenter(...) for a " +
                "parent-owned child presenter. If the presenter is instead exposed through a manual " +
                "@GraphExtension, add its simple name to `ktlint_tvmaniac_unrouted_presenters` in " +
                "`.editorconfig`."
    }
}
