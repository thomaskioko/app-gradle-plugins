package io.github.thomaskioko.gradle.plugins.lint.navigation

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class NoMutatingRouterImportRuleTest {
    private val assertThat = assertThatRule { NoMutatingRouterImportRule() }

    @Test
    fun `does not flag ChildStack import in features module`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.presenter.home

            import com.arkivanov.decompose.router.stack.ChildStack

            interface HomePresenter {
                fun stack(): ChildStack<String, Int>
            }
            """.trimIndent(),
        )
            .asFileWithPath("/repo/features/home/presenter/src/commonMain/kotlin/com/thomaskioko/tvmaniac/presenter/home/HomePresenter.kt")
            .hasNoLintViolations()
    }

    @Test
    fun `does not flag ChildSlot import in root presenter`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.presenter.root

            import com.arkivanov.decompose.router.slot.ChildSlot

            class RootPresenter
            """.trimIndent(),
        )
            .asFileWithPath("/repo/features/root/presenter/src/commonMain/kotlin/com/thomaskioko/tvmaniac/presenter/root/RootPresenter.kt")
            .hasNoLintViolations()
    }

    @Test
    fun `does not flag ChildStack import in feature ui`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.home.ui

            import com.arkivanov.decompose.router.stack.ChildStack

            class HomeScreen
            """.trimIndent(),
        )
            .asFileWithPath("/repo/features/home/ui/src/main/java/com/thomaskioko/tvmaniac/home/ui/HomeScreen.kt")
            .hasNoLintViolations()
    }

    @Test
    fun `flags StackNavigation mutation import in features`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.presenter.home

            import com.arkivanov.decompose.router.stack.StackNavigation

            class HomePresenter
            """.trimIndent(),
        )
            .asFileWithPath("/repo/features/home/presenter/src/commonMain/kotlin/com/thomaskioko/tvmaniac/presenter/home/HomePresenter.kt")
            .hasLintViolationWithoutAutoCorrect(
                line = 3,
                col = 1,
                detail = NoMutatingRouterImportRule.errorMessage("com.arkivanov.decompose.router.stack.StackNavigation"),
            )
    }

    @Test
    fun `flags pushNew mutation import in features test`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.presenter.show

            import com.arkivanov.decompose.router.stack.pushNew

            class ShowDetailsPresenterTest
            """.trimIndent(),
        )
            .asFileWithPath("/repo/features/show-details/presenter/src/commonTest/kotlin/com/thomaskioko/tvmaniac/presenter/show/ShowDetailsPresenterTest.kt")
            .hasLintViolationWithoutAutoCorrect(
                line = 3,
                col = 1,
                detail = NoMutatingRouterImportRule.errorMessage("com.arkivanov.decompose.router.stack.pushNew"),
            )
    }

    @Test
    fun `flags wildcard router import in features`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.presenter.home

            import com.arkivanov.decompose.router.stack.*

            class HomePresenter
            """.trimIndent(),
        )
            .asFileWithPath("/repo/features/home/presenter/src/commonMain/kotlin/com/thomaskioko/tvmaniac/presenter/home/HomePresenter.kt")
            .hasLintViolationWithoutAutoCorrect(
                line = 3,
                col = 1,
                detail = NoMutatingRouterImportRule.errorMessage("com.arkivanov.decompose.router.stack.*"),
            )
    }

    @Test
    fun `flags slot mutation import in core module`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.core

            import com.arkivanov.decompose.router.slot.activate

            class Helper
            """.trimIndent(),
        )
            .asFileWithPath("/repo/core/util/src/commonMain/kotlin/com/thomaskioko/tvmaniac/core/Helper.kt")
            .hasLintViolationWithoutAutoCorrect(
                line = 3,
                col = 1,
                detail = NoMutatingRouterImportRule.errorMessage("com.arkivanov.decompose.router.slot.activate"),
            )
    }

    @Test
    fun `does not flag mutation imports in navigation implementation`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.navigation

            import com.arkivanov.decompose.router.stack.StackNavigation
            import com.arkivanov.decompose.router.stack.pushNew
            import com.arkivanov.decompose.router.slot.SlotNavigation
            import com.arkivanov.decompose.router.slot.activate

            class DefaultNavigator
            """.trimIndent(),
        )
            .asFileWithPath("/repo/navigation/implementation/src/commonMain/kotlin/com/thomaskioko/tvmaniac/navigation/DefaultNavigator.kt")
            .hasNoLintViolations()
    }

    @Test
    fun `does not flag mutation imports in navigation testing fakes`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.navigation.testing

            import com.arkivanov.decompose.router.stack.StackNavigation
            import com.arkivanov.decompose.router.stack.bringToFront
            import com.arkivanov.decompose.router.slot.SlotNavigation

            class FakeNavigator
            """.trimIndent(),
        )
            .asFileWithPath("/repo/navigation/testing/src/commonMain/kotlin/com/thomaskioko/tvmaniac/navigation/testing/FakeNavigator.kt")
            .hasNoLintViolations()
    }

    @Test
    fun `does not flag unrelated imports`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.presenter.home

            import com.thomaskioko.tvmaniac.navigation.Navigator
            import kotlinx.coroutines.flow.StateFlow

            class HomePresenter
            """.trimIndent(),
        )
            .asFileWithPath("/repo/features/home/presenter/src/commonMain/kotlin/com/thomaskioko/tvmaniac/presenter/home/HomePresenter.kt")
            .hasNoLintViolations()
    }

    @Test
    fun `does not flag aliased ChildStack import`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.presenter.home

            import com.arkivanov.decompose.router.stack.ChildStack as Stack

            class HomePresenter
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

            import com.arkivanov.decompose.router.stack.StackNavigation
            import com.arkivanov.decompose.router.slot.SlotNavigation

            class DefaultNavigator
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

            import com.arkivanov.decompose.router.stack.StackNavigation

            class DefaultNavigator
            """.trimIndent(),
        )
            .asFileWithPath("/repo/navigation/implementation/src/commonMain/kotlin/com/thomaskioko/tvmaniac/navigation/DefaultNavigator.kt")
            .withEditorConfigOverride(NAVIGATION_MODULE_PATHS_PROPERTY to "routing")
            .hasLintViolationWithoutAutoCorrect(
                line = 3,
                col = 1,
                detail = NoMutatingRouterImportRule.errorMessage("com.arkivanov.decompose.router.stack.StackNavigation"),
            )
    }

    @Test
    fun `should skip both segments given multi value override`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.navigation

            import com.arkivanov.decompose.router.stack.StackNavigation

            class FirstNavigator
            """.trimIndent(),
        )
            .asFileWithPath("/repo/nav/api/src/commonMain/kotlin/com/thomaskioko/tvmaniac/navigation/FirstNavigator.kt")
            .withEditorConfigOverride(NAVIGATION_MODULE_PATHS_PROPERTY to "nav,routing")
            .hasNoLintViolations()

        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.routing

            import com.arkivanov.decompose.router.slot.SlotNavigation

            class SecondNavigator
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

            import com.arkivanov.decompose.router.stack.StackNavigation

            class DefaultNavigator
            """.trimIndent(),
        )
            .asFileWithPath("/repo/navigation/implementation/src/commonMain/kotlin/com/thomaskioko/tvmaniac/navigation/DefaultNavigator.kt")
            .withEditorConfigOverride(NAVIGATION_MODULE_PATHS_PROPERTY to "")
            .hasLintViolationWithoutAutoCorrect(
                line = 3,
                col = 1,
                detail = NoMutatingRouterImportRule.errorMessage("com.arkivanov.decompose.router.stack.StackNavigation"),
            )
    }

    @Test
    fun `should treat slash wrapped value as bare segment`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.navigation

            import com.arkivanov.decompose.router.stack.StackNavigation

            class DefaultNavigator
            """.trimIndent(),
        )
            .asFileWithPath("/repo/navigation/implementation/src/commonMain/kotlin/com/thomaskioko/tvmaniac/navigation/DefaultNavigator.kt")
            .withEditorConfigOverride(NAVIGATION_MODULE_PATHS_PROPERTY to "/navigation/")
            .hasNoLintViolations()
    }
}
