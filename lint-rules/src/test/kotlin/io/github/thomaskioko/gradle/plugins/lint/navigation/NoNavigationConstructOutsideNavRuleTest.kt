package io.github.thomaskioko.gradle.plugins.lint.navigation

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class NoNavigationConstructOutsideNavRuleTest {
    private val assertThat = assertThatRule { NoNavigationConstructOutsideNavRule() }

    @Test
    fun `flags StackNavigation construction in features module`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.presenter.home

            class HomePresenter {
                private val stack = StackNavigation<String>()
            }
            """.trimIndent(),
        )
            .asFileWithPath("/repo/features/home/presenter/src/commonMain/kotlin/com/thomaskioko/tvmaniac/presenter/home/HomePresenter.kt")
            .hasLintViolationWithoutAutoCorrect(
                line = 4,
                col = 25,
                detail = NoNavigationConstructOutsideNavRule.errorMessage("StackNavigation"),
            )
    }

    @Test
    fun `flags SlotNavigation construction in core module`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.core

            class Helper {
                private val sheet = SlotNavigation<String>()
            }
            """.trimIndent(),
        )
            .asFileWithPath("/repo/core/util/src/commonMain/kotlin/com/thomaskioko/tvmaniac/core/Helper.kt")
            .hasLintViolationWithoutAutoCorrect(
                line = 4,
                col = 25,
                detail = NoNavigationConstructOutsideNavRule.errorMessage("SlotNavigation"),
            )
    }

    @Test
    fun `does not flag construction in navigation implementation`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.navigation

            class DefaultNavigator {
                private val stack = StackNavigation<String>()
                private val sheet = SlotNavigation<String>()
            }
            """.trimIndent(),
        )
            .asFileWithPath("/repo/navigation/implementation/src/commonMain/kotlin/com/thomaskioko/tvmaniac/navigation/DefaultNavigator.kt")
            .hasNoLintViolations()
    }

    @Test
    fun `does not flag construction in navigation testing fakes`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.navigation.testing

            class FakeNavigator {
                private val stack = StackNavigation<String>()
                private val sheet = SlotNavigation<String>()
            }
            """.trimIndent(),
        )
            .asFileWithPath("/repo/navigation/testing/src/commonMain/kotlin/com/thomaskioko/tvmaniac/navigation/testing/FakeNavigator.kt")
            .hasNoLintViolations()
    }

    @Test
    fun `does not flag construction in navigation api`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.navigation

            class NavigatorBuilder {
                fun build() = StackNavigation<String>()
            }
            """.trimIndent(),
        )
            .asFileWithPath("/repo/navigation/api/src/commonMain/kotlin/com/thomaskioko/tvmaniac/navigation/NavigatorBuilder.kt")
            .hasNoLintViolations()
    }

    @Test
    fun `does not flag StackNavigation type reference without construction`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.presenter.home

            interface HomePresenter {
                fun navigate(stack: StackNavigation<String>)
            }
            """.trimIndent(),
        )
            .asFileWithPath("/repo/features/home/presenter/src/commonMain/kotlin/com/thomaskioko/tvmaniac/presenter/home/HomePresenter.kt")
            .hasNoLintViolations()
    }

    @Test
    fun `does not flag unrelated calls`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.presenter.home

            class HomePresenter {
                private val list = mutableListOf<String>()
                init { println("hello") }
            }
            """.trimIndent(),
        )
            .asFileWithPath("/repo/features/home/presenter/src/commonMain/kotlin/com/thomaskioko/tvmaniac/presenter/home/HomePresenter.kt")
            .hasNoLintViolations()
    }

    @Test
    fun `should skip override path given single custom navigation module`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.routing

            class DefaultNavigator {
                private val stack = StackNavigation<String>()
                private val sheet = SlotNavigation<String>()
            }
            """.trimIndent(),
        )
            .asFileWithPath("/repo/routing/implementation/src/commonMain/kotlin/com/thomaskioko/tvmaniac/routing/DefaultNavigator.kt")
            .withEditorConfigOverride(NAVIGATION_MODULE_PATHS_PROPERTY to "routing")
            .hasNoLintViolations()
    }

    @Test
    fun `should still flag default navigation path when override replaces default`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.navigation

            class DefaultNavigator {
                private val stack = StackNavigation<String>()
            }
            """.trimIndent(),
        )
            .asFileWithPath("/repo/navigation/implementation/src/commonMain/kotlin/com/thomaskioko/tvmaniac/navigation/DefaultNavigator.kt")
            .withEditorConfigOverride(NAVIGATION_MODULE_PATHS_PROPERTY to "routing")
            .hasLintViolationWithoutAutoCorrect(
                line = 4,
                col = 25,
                detail = NoNavigationConstructOutsideNavRule.errorMessage("StackNavigation"),
            )
    }

    @Test
    fun `should skip both segments given multi value override`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.navigation

            class FirstNavigator {
                private val stack = StackNavigation<String>()
            }
            """.trimIndent(),
        )
            .asFileWithPath("/repo/nav/api/src/commonMain/kotlin/com/thomaskioko/tvmaniac/navigation/FirstNavigator.kt")
            .withEditorConfigOverride(NAVIGATION_MODULE_PATHS_PROPERTY to "nav,routing")
            .hasNoLintViolations()

        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.routing

            class SecondNavigator {
                private val sheet = SlotNavigation<String>()
            }
            """.trimIndent(),
        )
            .asFileWithPath("/repo/routing/implementation/src/commonMain/kotlin/com/thomaskioko/tvmaniac/routing/SecondNavigator.kt")
            .withEditorConfigOverride(NAVIGATION_MODULE_PATHS_PROPERTY to "nav,routing")
            .hasNoLintViolations()
    }

    @Test
    fun `should flag every path given empty override`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.navigation

            class DefaultNavigator {
                private val stack = StackNavigation<String>()
            }
            """.trimIndent(),
        )
            .asFileWithPath("/repo/navigation/implementation/src/commonMain/kotlin/com/thomaskioko/tvmaniac/navigation/DefaultNavigator.kt")
            .withEditorConfigOverride(NAVIGATION_MODULE_PATHS_PROPERTY to "")
            .hasLintViolationWithoutAutoCorrect(
                line = 4,
                col = 25,
                detail = NoNavigationConstructOutsideNavRule.errorMessage("StackNavigation"),
            )
    }

    @Test
    fun `should treat slash wrapped value as bare segment`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.navigation

            class DefaultNavigator {
                private val stack = StackNavigation<String>()
            }
            """.trimIndent(),
        )
            .asFileWithPath("/repo/navigation/implementation/src/commonMain/kotlin/com/thomaskioko/tvmaniac/navigation/DefaultNavigator.kt")
            .withEditorConfigOverride(NAVIGATION_MODULE_PATHS_PROPERTY to "/navigation/")
            .hasNoLintViolations()
    }
}
