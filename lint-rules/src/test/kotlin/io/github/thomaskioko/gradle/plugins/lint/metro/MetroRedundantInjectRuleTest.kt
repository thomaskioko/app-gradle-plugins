package io.github.thomaskioko.gradle.plugins.lint.metro

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class MetroRedundantInjectRuleTest {
    private val assertThat = assertThatRule { MetroRedundantInjectRule() }

    @Test
    fun `removes class-level Inject when ContributesBinding is present`() {
        val before =
            """
            package test

            @ContributesBinding(AppScope::class)
            @Inject
            class FooImpl : Foo
            """.trimIndent()
        val after =
            """
            package test

            @ContributesBinding(AppScope::class)
            class FooImpl : Foo
            """.trimIndent()
        assertThat(before)
            .hasLintViolation(
                line = 4,
                col = 1,
                detail = MetroRedundantInjectRule.ERROR_MESSAGE,
            ).isFormattedAs(after)
    }

    @Test
    fun `removes Inject when it precedes ContributesBinding`() {
        val before =
            """
            package test

            @Inject
            @ContributesBinding(AppScope::class)
            class FooImpl : Foo
            """.trimIndent()
        val after =
            """
            package test

            @ContributesBinding(AppScope::class)
            class FooImpl : Foo
            """.trimIndent()
        assertThat(before)
            .hasLintViolation(
                line = 3,
                col = 1,
                detail = MetroRedundantInjectRule.ERROR_MESSAGE,
            ).isFormattedAs(after)
    }

    @Test
    fun `removes Inject when ContributesIntoSet is present`() {
        val before =
            """
            package test

            @ContributesIntoSet(AppScope::class)
            @Inject
            class FooImpl : Foo
            """.trimIndent()
        val after =
            """
            package test

            @ContributesIntoSet(AppScope::class)
            class FooImpl : Foo
            """.trimIndent()
        assertThat(before)
            .hasLintViolation(
                line = 4,
                col = 1,
                detail = MetroRedundantInjectRule.ERROR_MESSAGE,
            ).isFormattedAs(after)
    }

    @Test
    fun `removes Inject when ContributesIntoMap is present`() {
        val before =
            """
            package test

            @ContributesIntoMap(AppScope::class)
            @Inject
            class FooImpl : Foo
            """.trimIndent()
        val after =
            """
            package test

            @ContributesIntoMap(AppScope::class)
            class FooImpl : Foo
            """.trimIndent()
        assertThat(before)
            .hasLintViolation(
                line = 4,
                col = 1,
                detail = MetroRedundantInjectRule.ERROR_MESSAGE,
            ).isFormattedAs(after)
    }

    @Test
    fun `removes Inject when ContributesTo is present`() {
        val before =
            """
            package test

            @ContributesTo(AppScope::class)
            @Inject
            class FooImpl
            """.trimIndent()
        val after =
            """
            package test

            @ContributesTo(AppScope::class)
            class FooImpl
            """.trimIndent()
        assertThat(before)
            .hasLintViolation(
                line = 4,
                col = 1,
                detail = MetroRedundantInjectRule.ERROR_MESSAGE,
            ).isFormattedAs(after)
    }

    @Test
    fun `removes constructor-level Inject when ContributesBinding is on the class`() {
        val before =
            """
            package test

            @ContributesBinding(AppScope::class)
            class FooImpl @Inject constructor(
                private val dep: Dep,
            ) : Foo
            """.trimIndent()
        val after =
            """
            package test

            @ContributesBinding(AppScope::class)
            class FooImpl constructor(
                private val dep: Dep,
            ) : Foo
            """.trimIndent()
        assertThat(before)
            .hasLintViolation(
                line = 4,
                col = 15,
                detail = MetroRedundantInjectRule.ERROR_MESSAGE,
            ).isFormattedAs(after)
    }

    @Test
    fun `removes class-level Inject with constructor params when ContributesBinding is present`() {
        val before =
            """
            package test

            @ContributesBinding(AppScope::class)
            @Inject
            class FooImpl(
                private val dep: Dep,
            ) : Foo
            """.trimIndent()
        val after =
            """
            package test

            @ContributesBinding(AppScope::class)
            class FooImpl(
                private val dep: Dep,
            ) : Foo
            """.trimIndent()
        assertThat(before)
            .hasLintViolation(
                line = 4,
                col = 1,
                detail = MetroRedundantInjectRule.ERROR_MESSAGE,
            ).isFormattedAs(after)
    }

    @Test
    fun `does not flag class with only Inject`() {
        assertThat(
            // language=kotlin
            """
            package test

            @Inject
            class FooImpl(private val dep: Dep) : Foo
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag class with only ContributesBinding`() {
        assertThat(
            // language=kotlin
            """
            package test

            @ContributesBinding(AppScope::class)
            class FooImpl : Foo
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag class with only ContributesTo`() {
        assertThat(
            // language=kotlin
            """
            package test

            @ContributesTo(AppScope::class)
            class FooImpl
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag class with only constructor Inject`() {
        assertThat(
            // language=kotlin
            """
            package test

            class FooImpl @Inject constructor(
                private val dep: Dep,
            ) : Foo
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag unannotated class`() {
        assertThat(
            // language=kotlin
            """
            package test

            class FooImpl : Foo
            """.trimIndent(),
        ).hasNoLintViolations()
    }
}
