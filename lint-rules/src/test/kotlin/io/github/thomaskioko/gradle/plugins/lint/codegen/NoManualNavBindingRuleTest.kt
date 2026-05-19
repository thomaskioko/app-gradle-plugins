package io.github.thomaskioko.gradle.plugins.lint.codegen

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class NoManualNavBindingRuleTest {
    private val assertThat = assertThatRule { NoManualNavBindingRule() }

    @Test
    fun `should flag provider given Provides IntoSet returning NavRoot`() {
        assertThat(
            // language=kotlin
            """
            package test

            @ContributesTo(ActivityScope::class)
            interface ProgressRootBinding {
                companion object {
                    @Provides
                    @IntoSet
                    fun provideProgressRoot(): NavRoot = ProgressRoot
                }
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 6,
            col = 9,
            detail = NoManualNavBindingRule.errorMessage("NavRoot", "provideProgressRoot"),
        )
    }

    @Test
    fun `should flag provider given Provides IntoSet returning NavRootBinding`() {
        assertThat(
            // language=kotlin
            """
            package test

            @ContributesTo(ActivityScope::class)
            interface ProgressRootBinding {
                companion object {
                    @Provides
                    @IntoSet
                    fun provideProgressRootBinding(): NavRootBinding<*> =
                        NavRootBinding(ProgressRoot::class, ProgressRoot.serializer())
                }
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 6,
            col = 9,
            detail = NoManualNavBindingRule.errorMessage("NavRootBinding", "provideProgressRootBinding"),
        )
    }

    @Test
    fun `should flag provider given Provides IntoSet returning NavDestination`() {
        assertThat(
            // language=kotlin
            """
            package test

            @ContributesTo(ActivityScope::class)
            interface GenreShowsNavDestinationBinding {
                companion object {
                    @Provides
                    @IntoSet
                    fun provideGenreShowsNavDestination(): NavDestination<*> = NavDestination.Screen(
                        routeClass = GenreShowsRoute::class,
                    ) { _, _ -> GenreShowsDestination }
                }
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 6,
            col = 9,
            detail = NoManualNavBindingRule.errorMessage("NavDestination", "provideGenreShowsNavDestination"),
        )
    }

    @Test
    fun `should flag provider given Provides IntoSet returning NavRouteBinding`() {
        assertThat(
            // language=kotlin
            """
            package test

            @ContributesTo(ActivityScope::class)
            interface GenreShowsNavDestinationBinding {
                companion object {
                    @Provides
                    @IntoSet
                    fun provideGenreShowsRouteBinding(): NavRouteBinding<*> =
                        NavRouteBinding(GenreShowsRoute::class, GenreShowsRoute.serializer())
                }
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 6,
            col = 9,
            detail = NoManualNavBindingRule.errorMessage("NavRouteBinding", "provideGenreShowsRouteBinding"),
        )
    }

    @Test
    fun `should flag provider given IntoSet annotation precedes Provides`() {
        assertThat(
            // language=kotlin
            """
            package test

            @ContributesTo(ActivityScope::class)
            interface LibraryRootBinding {
                companion object {
                    @IntoSet
                    @Provides
                    fun provideLibraryRoot(): NavRoot = LibraryRoot
                }
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 6,
            col = 9,
            detail = NoManualNavBindingRule.errorMessage("NavRoot", "provideLibraryRoot"),
        )
    }

    @Test
    fun `should flag provider given top-level Provides IntoSet function`() {
        assertThat(
            // language=kotlin
            """
            package test

            @Provides
            @IntoSet
            fun provideStrayNavRoot(): NavRoot = StrayRoot
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 3,
            col = 1,
            detail = NoManualNavBindingRule.errorMessage("NavRoot", "provideStrayNavRoot"),
        )
    }

    @Test
    fun `should not flag provider given Provides IntoSet returning unrelated type`() {
        assertThat(
            // language=kotlin
            """
            package test

            @ContributesTo(ActivityScope::class)
            interface DebugNotificationInitializerBindingContainer {
                companion object {
                    @Provides
                    @IntoSet
                    fun provideInitializer(): Initializer = DebugInitializer
                }
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `should not flag provider given Provides without IntoSet`() {
        assertThat(
            // language=kotlin
            """
            package test

            @ContributesTo(ActivityScope::class)
            interface SomeContainer {
                companion object {
                    @Provides
                    fun provideRoot(): NavRoot = SomeRoot
                }
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `should not flag provider given IntoSet without Provides`() {
        assertThat(
            // language=kotlin
            """
            package test

            @ContributesTo(ActivityScope::class)
            interface SomeContainer {
                companion object {
                    @IntoSet
                    fun provideRoot(): NavRoot = SomeRoot
                }
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `should not flag function given no annotations even when returning NavRoot`() {
        assertThat(
            // language=kotlin
            """
            package test

            fun describeNavRoot(): NavRoot = SomeRoot
            """.trimIndent(),
        ).hasNoLintViolations()
    }
}
