package io.github.thomaskioko.gradle.plugins.lint.codegen

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class ComposeScreenCodegenAnnotationRuleTest {
    private val assertThat = assertThatRule { ComposeScreenCodegenAnnotationRule() }

    @Test
    fun `flags Composable with presenter parameter without codegen annotation`() {
        assertThat(
            // language=kotlin
            """
            package test

            @Composable
            fun TrendingShowsScreen(
                presenter: TrendingShowsPresenter,
                modifier: Modifier = Modifier,
            ) {
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 3,
            col = 1,
            detail = ComposeScreenCodegenAnnotationRule.errorMessage("TrendingShowsScreen"),
        )
    }

    @Test
    fun `does not flag Composable with ScreenUi annotation`() {
        assertThat(
            // language=kotlin
            """
            package test

            @ScreenUi(presenter = TrendingShowsPresenter::class, parentScope = ActivityScope::class)
            @Composable
            fun TrendingShowsScreen(
                presenter: TrendingShowsPresenter,
                modifier: Modifier = Modifier,
            ) {
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag Composable with SheetUi annotation`() {
        assertThat(
            // language=kotlin
            """
            package test

            @SheetUi(presenter = EpisodeSheetPresenter::class, parentScope = ActivityScope::class)
            @Composable
            fun EpisodeSheet(
                presenter: EpisodeSheetPresenter,
                modifier: Modifier = Modifier,
            ) {
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag Composable with AppRootUi annotation`() {
        assertThat(
            // language=kotlin
            """
            package test

            @AppRootUi(presenter = RootPresenter::class, parentScope = ActivityScope::class)
            @Composable
            fun RootScreen(
                rootPresenter: RootPresenter,
                screenContents: Set<ScreenContent>,
                sheetContents: Set<SheetContent>,
                modifier: Modifier = Modifier,
            ) {
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag Composable without presenter parameter`() {
        assertThat(
            // language=kotlin
            """
            package test

            @Composable
            fun GreetingCard(
                title: String,
                modifier: Modifier = Modifier,
            ) {
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag function without Composable annotation`() {
        assertThat(
            // language=kotlin
            """
            package test

            fun setupPresenter(
                presenter: SomePresenter,
            ) {
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag Composable function listed in unrouted_screens`() {
        assertThat(
            // language=kotlin
            """
            package test

            @Composable
            fun DiscoverScreen(
                presenter: DiscoverShowsPresenter,
                modifier: Modifier = Modifier,
            ) {
            }
            """.trimIndent(),
        )
            .withEditorConfigOverride(UNROUTED_SCREENS_PROPERTY to "DiscoverScreen")
            .hasNoLintViolations()
    }

    @Test
    fun `flags Composable host that uses rootPresenter parameter name`() {
        assertThat(
            // language=kotlin
            """
            package test

            @Composable
            fun RootScreen(
                rootPresenter: RootPresenter,
                modifier: Modifier = Modifier,
            ) {
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 3,
            col = 1,
            detail = ComposeScreenCodegenAnnotationRule.errorMessage("RootScreen"),
        )
    }

    @Test
    fun `does not flag nested Composable inside another function`() {
        assertThat(
            // language=kotlin
            """
            package test

            @Composable
            @ScreenUi(presenter = HomePresenter::class, parentScope = ActivityScope::class)
            fun HomeScreen(
                presenter: HomePresenter,
                modifier: Modifier = Modifier,
            ) {
                @Composable
                fun nested(
                    presenter: HomePresenter,
                ) {
                }
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }
}
