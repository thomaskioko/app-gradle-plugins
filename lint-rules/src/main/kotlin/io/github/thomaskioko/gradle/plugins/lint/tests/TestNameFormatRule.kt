package io.github.thomaskioko.gradle.plugins.lint.tests

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import io.github.thomaskioko.gradle.plugins.lint.RULE_ABOUT
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Enforces the BDD(Behavior-Driven Development) style test naming convention `should X given Y`
 * (or `should X when Y`) on every `@Test` annotated function.
 *
 * The rule supports both backticked names (`fun \`should emit state given no data\`()`) and
 * camelCase names (`fun shouldEmitStateGivenNoData()`). The camelCase form is needed for files
 * under `src/androidTest/` because DEX format 037 forbids spaces in identifiers.
 *
 * Recognised test annotations: `@Test` (`kotlin.test`, `org.junit`, `org.junit.jupiter.api`),
 * `@ParameterizedTest`, and `@RepeatedTest`.
 *
 * Lifecycle annotations such as `@BeforeTest`, `@AfterTest`, `@BeforeAll`, and `@AfterEach` are
 * ignored. Those are setup and teardown hooks, not tests.
 *
 * ## Example
 *
 * ```kotlin
 * // Allowed:
 * @Test fun `should emit initial state given no data`() {}
 * @Test fun shouldRenderHomeScreenGivenAuthenticatedUser() {}
 *
 * // Forbidden:
 * @Test fun `initial active root should be Discover`() {}             // missing `should` prefix
 * @Test fun `should display correct watch progress percentage`() {}    // missing `given` or `when`
 * ```
 */
public class TestNameFormatRule :
    Rule(
        ruleId = RuleId("tvmaniac:test-name-format"),
        about = RULE_ABOUT,
    ),
    RuleAutocorrectApproveHandler {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != FUN) return

        val function = node.psi as? KtNamedFunction ?: return
        if (!function.hasTestAnnotation()) return

        val name = function.name ?: return
        if (name.matchesTestNameFormat()) return

        emit(node.startOffset, errorMessage(name), false)
    }

    private fun KtNamedFunction.hasTestAnnotation(): Boolean =
        annotationEntries.any { entry ->
            entry.shortName?.asString() in TEST_ANNOTATIONS
        }

    private fun String.matchesTestNameFormat(): Boolean = if (contains(" ")) {
        // Backticked form: `should X given Y` or `should X when Y`
        startsWith("should ") && (contains(" given ") || contains(" when "))
    } else {
        // CamelCase form (used in src/androidTest because DEX 037 forbids spaces in identifiers)
        startsWith("should") &&
            length > "should".length &&
            this["should".length].isUpperCase() &&
            (contains("Given") || contains("When"))
    }

    public companion object {
        internal val TEST_ANNOTATIONS: Set<String> = setOf(
            "Test",
            "ParameterizedTest",
            "RepeatedTest",
        )

        /**
         * Builds the lint error message for a test name that does not follow the convention.
         *
         * @param name The function name that violated the convention.
         */
        public fun errorMessage(name: String): String =
            "Test name '$name' does not follow the project convention. " +
                "Use 'should X given Y' (backticked) or 'shouldXGivenY' / 'shouldXWhenY' (camelCase). " +
                "The name must start with 'should' and contain 'given' or 'when'."
    }
}
