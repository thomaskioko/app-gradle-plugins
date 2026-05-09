package io.github.thomaskioko.gradle.plugins.lint.navigation

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class NoCustomNavigatorInterfaceRuleTest {
    private val assertThat = assertThatRule { NoCustomNavigatorInterfaceRule() }

    @Test
    fun `flags custom Navigator interface in features`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.presenter.show

            interface ShowDetailsNavigator
            """.trimIndent(),
        )
            .asFileWithPath("/repo/features/show-details/presenter/src/commonMain/kotlin/com/thomaskioko/tvmaniac/presenter/show/ShowDetailsNavigator.kt")
            .hasLintViolationWithoutAutoCorrect(
                line = 3,
                col = 1,
                detail = NoCustomNavigatorInterfaceRule.errorMessage("ShowDetailsNavigator"),
            )
    }

    @Test
    fun `flags custom Navigator interface in any module`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.core.notifications

            interface NotificationNavigator
            """.trimIndent(),
        )
            .asFileWithPath("/repo/core/notifications/src/commonMain/kotlin/com/thomaskioko/tvmaniac/core/notifications/NotificationNavigator.kt")
            .hasLintViolationWithoutAutoCorrect(
                line = 3,
                col = 1,
                detail = NoCustomNavigatorInterfaceRule.errorMessage("NotificationNavigator"),
            )
    }

    @Test
    fun `does not flag canonical Navigator interface`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.navigation

            interface Navigator
            """.trimIndent(),
        )
            .asFileWithPath("/repo/navigation/api/src/commonMain/kotlin/com/thomaskioko/tvmaniac/navigation/Navigator.kt")
            .hasNoLintViolations()
    }

    @Test
    fun `does not flag canonical SheetNavigator interface`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.navigation

            interface SheetNavigator
            """.trimIndent(),
        )
            .asFileWithPath("/repo/navigation/api/src/commonMain/kotlin/com/thomaskioko/tvmaniac/navigation/SheetNavigator.kt")
            .hasNoLintViolations()
    }

    @Test
    fun `flags reintroduced HomeTabNavigator interface`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.navigation

            interface HomeTabNavigator
            """.trimIndent(),
        )
            .asFileWithPath("/repo/navigation/api/src/commonMain/kotlin/com/thomaskioko/tvmaniac/navigation/HomeTabNavigator.kt")
            .hasLintViolationWithoutAutoCorrect(
                line = 3,
                col = 1,
                detail = NoCustomNavigatorInterfaceRule.errorMessage("HomeTabNavigator"),
            )
    }

    @Test
    fun `does not flag classes that end with Navigator`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.navigation

            class DefaultNavigator
            """.trimIndent(),
        )
            .asFileWithPath("/repo/navigation/implementation/src/commonMain/kotlin/com/thomaskioko/tvmaniac/navigation/DefaultNavigator.kt")
            .hasNoLintViolations()
    }

    @Test
    fun `does not flag interfaces that do not end with Navigator`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.presenter.home

            interface HomePresenter
            """.trimIndent(),
        )
            .asFileWithPath("/repo/features/home/presenter/src/commonMain/kotlin/com/thomaskioko/tvmaniac/presenter/home/HomePresenter.kt")
            .hasNoLintViolations()
    }
}
