/*
 * Copyright 2026 Thomas Kioko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
