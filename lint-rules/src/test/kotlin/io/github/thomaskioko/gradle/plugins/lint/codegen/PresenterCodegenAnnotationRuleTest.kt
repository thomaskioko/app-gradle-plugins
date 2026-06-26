package io.github.thomaskioko.gradle.plugins.lint.codegen

import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import java.io.File
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class PresenterCodegenAnnotationRuleTest {
    private val assertThat = assertThatRule { PresenterCodegenAnnotationRule() }

    @Test
    fun `flags Inject presenter without codegen annotation`() {
        assertThat(
            // language=kotlin
            """
            package test

            @Inject
            class TrendingShowsPresenter(
                componentContext: ComponentContext,
            )
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 3,
            col = 1,
            detail = PresenterCodegenAnnotationRule.errorMessage("TrendingShowsPresenter"),
        )
    }

    @Test
    fun `flags AssistedInject presenter without codegen annotation`() {
        assertThat(
            // language=kotlin
            """
            package test

            @AssistedInject
            class ShowDetailsPresenter(
                componentContext: ComponentContext,
            )
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 3,
            col = 1,
            detail = PresenterCodegenAnnotationRule.errorMessage("ShowDetailsPresenter"),
        )
    }

    @Test
    fun `does not flag Inject presenter with NavDestination`() {
        assertThat(
            // language=kotlin
            """
            package test

            @Inject
            @NavDestination(
                route = TrendingShowsRoute::class,
                parentScope = ActivityScope::class,
                kind = DestinationKind.SCREEN,
            )
            class TrendingShowsPresenter(
                componentContext: ComponentContext,
            )
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag presenter with NavDestinationAnno alias`() {
        assertThat(
            // language=kotlin
            """
            package test

            @NavDestinationAnno(
                route = HomeRoute::class,
                parentScope = ActivityScope::class,
                kind = DestinationKind.SCREEN,
            )
            @Inject
            class HomePresenter(
                componentContext: ComponentContext,
            )
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag AssistedInject presenter with AppRoot`() {
        assertThat(
            // language=kotlin
            """
            package test

            @AppRoot(parentScope = ActivityScope::class)
            @AssistedInject
            class DefaultRootPresenter(
                @Assisted componentContext: ComponentContext,
            ) : RootPresenter
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag presenter with ContributesBinding`() {
        assertThat(
            // language=kotlin
            """
            package test

            @ContributesBinding(AppScope::class)
            @Inject
            class DefaultLogoutPresenter(
                componentContext: ComponentContext,
            ) : LogoutPresenter
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag presenter interface`() {
        assertThat(
            // language=kotlin
            """
            package test

            interface RootPresenter {
                fun onClicked()
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag abstract presenter base class`() {
        assertThat(
            // language=kotlin
            """
            package test

            @Inject
            abstract class BaseFeaturePresenter(
                componentContext: ComponentContext,
            )
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag class without presenter suffix`() {
        assertThat(
            // language=kotlin
            """
            package test

            @Inject
            class TrendingShowsRepository(
                componentContext: ComponentContext,
            )
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag presenter without injection annotation`() {
        assertThat(
            // language=kotlin
            """
            package test

            class FakeTrendingShowsPresenter(
                componentContext: ComponentContext,
            )
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag presenter listed in unrouted_presenters`() {
        assertThat(
            // language=kotlin
            """
            package test

            @Inject
            class UpNextPresenter(
                componentContext: ComponentContext,
            )
            """.trimIndent(),
        )
            .withEditorConfigOverride(UNROUTED_PRESENTERS_PROPERTY to "UpNextPresenter")
            .hasNoLintViolations()
    }

    @Test
    fun `does not flag presenter listed in unrouted_presenters parsed from a real editorconfig`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, ".editorconfig").writeText(
            """
            root = true

            [*.kt]
            ktlint_tvmaniac_unrouted_presenters = UpNextPresenter
            """.trimIndent(),
        )
        val ktFile = File(tempDir, "UpNextPresenter.kt")
        ktFile.writeText(
            """
            package test

            @Inject
            class UpNextPresenter(
                componentContext: ComponentContext,
            )
            """.trimIndent(),
        )

        val engine = KtLintRuleEngine(
            ruleProviders = setOf(RuleProvider { PresenterCodegenAnnotationRule() }),
        )
        val errors = mutableListOf<LintError>()
        engine.lint(Code.fromFile(ktFile)) { errors.add(it) }

        assertTrue(errors.isEmpty()) {
            "PascalCase .editorconfig exemption should suppress the rule via the ec4j parse path, but got: $errors"
        }
    }

    @Test
    fun `does not flag presenter whose unrouted_presenters entry differs in case from the declared name`(
        @TempDir tempDir: File,
    ) {
        File(tempDir, ".editorconfig").writeText(
            """
            root = true

            [*.kt]
            ktlint_tvmaniac_unrouted_presenters = upnextpresenter
            """.trimIndent(),
        )
        val ktFile = File(tempDir, "UpNextPresenter.kt")
        ktFile.writeText(
            """
            package test

            @Inject
            class UpNextPresenter(
                componentContext: ComponentContext,
            )
            """.trimIndent(),
        )

        val engine = KtLintRuleEngine(
            ruleProviders = setOf(RuleProvider { PresenterCodegenAnnotationRule() }),
        )
        val errors = mutableListOf<LintError>()
        engine.lint(Code.fromFile(ktFile)) { errors.add(it) }

        assertTrue(errors.isEmpty()) {
            "A lowercase exemption entry must still suppress the PascalCase presenter (case-insensitive match), but got: $errors"
        }
    }
}
