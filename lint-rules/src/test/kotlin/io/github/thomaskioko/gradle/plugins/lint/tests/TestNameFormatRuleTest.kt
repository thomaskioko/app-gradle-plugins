package io.github.thomaskioko.gradle.plugins.lint.tests

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class TestNameFormatRuleTest {
    private val assertThat = assertThatRule { TestNameFormatRule() }

    @Test
    fun `does not flag backticked should given test name`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac

            class CalendarPresenterTest {
                @Test
                fun `should emit initial state given no data`() {}
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag backticked should when test name`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac

            class CalendarPresenterTest {
                @Test
                fun `should refresh when RefreshCalendar is dispatched`() {}
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag camelCase shouldGiven test name`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac

            class HomeActivityTest {
                @Test
                fun shouldRenderHomeScreenGivenAuthenticatedUser() {}
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `flags backticked test missing should prefix`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac

            class ProgressPresenterTest {
                @Test
                fun `initial active root should be Discover`() {}
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 4,
            col = 5,
            detail = TestNameFormatRule.errorMessage("initial active root should be Discover"),
        )
    }

    @Test
    fun `flags backticked test missing given or when`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac

            class WatchlistPresenterTest {
                @Test
                fun `should display correct watch progress percentage`() {}
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 4,
            col = 5,
            detail = TestNameFormatRule.errorMessage("should display correct watch progress percentage"),
        )
    }

    @Test
    fun `flags camelCase test missing Given or When`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac

            class HomeActivityTest {
                @Test
                fun shouldRenderHomeScreen() {}
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 4,
            col = 5,
            detail = TestNameFormatRule.errorMessage("shouldRenderHomeScreen"),
        )
    }

    @Test
    fun `flags backticked test starting with given instead of should`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac

            class SeasonsPresenterTest {
                @Test
                fun `given result is success correct state is emitted`() {}
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 4,
            col = 5,
            detail = TestNameFormatRule.errorMessage("given result is success correct state is emitted"),
        )
    }

    @Test
    fun `does not flag BeforeTest lifecycle method`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac

            class CalendarPresenterTest {
                @BeforeTest
                fun setUp() {}

                @AfterTest
                fun tearDown() {}
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag non-test functions`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac

            class CalendarPresenterTest {
                private fun createPresenter() = Unit
                private fun todayEpochMillis(): Long = 0L
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag ParameterizedTest with valid format`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac

            class FormatterTest {
                @ParameterizedTest
                fun `should format episode given runtime`() {}
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `flags ParameterizedTest with invalid format`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac

            class FormatterTest {
                @ParameterizedTest
                fun `format episode runtime`() {}
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 4,
            col = 5,
            detail = TestNameFormatRule.errorMessage("format episode runtime"),
        )
    }
}
