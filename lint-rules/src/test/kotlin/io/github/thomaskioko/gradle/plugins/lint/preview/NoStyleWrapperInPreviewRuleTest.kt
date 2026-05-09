package io.github.thomaskioko.gradle.plugins.lint.preview

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class NoStyleWrapperInPreviewRuleTest {
    private val assertThat = assertThatRule { NoStyleWrapperInPreviewRule() }

    @Test
    fun `flags TvManiacTheme wrapper inside Preview`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.discover.ui

            @Preview
            @Composable
            private fun DiscoverScreenPreview() {
                TvManiacTheme {
                    DiscoverScreen()
                }
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 6,
            col = 5,
            detail = NoStyleWrapperInPreviewRule.errorMessage("TvManiacTheme", "DiscoverScreenPreview"),
        )
    }

    @Test
    fun `flags TvManiacBackground wrapper inside PreviewLightDark`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.discover.ui

            @PreviewLightDark
            @Composable
            private fun NextEpisodeCardPreview() {
                TvManiacBackground {
                    NextEpisodeCard()
                }
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 6,
            col = 5,
            detail = NoStyleWrapperInPreviewRule.errorMessage("TvManiacBackground", "NextEpisodeCardPreview"),
        )
    }

    @Test
    fun `flags wrapper inside custom multi-preview annotation`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.settings.ui

            @ThemePreviews
            @Composable
            private fun ThemeSwatchPreview() {
                TvManiacTheme {
                    ThemeSwatch()
                }
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 6,
            col = 5,
            detail = NoStyleWrapperInPreviewRule.errorMessage("TvManiacTheme", "ThemeSwatchPreview"),
        )
    }

    @Test
    fun `flags Surface wrapper inside Preview by default`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.discover.ui

            @Preview
            @Composable
            private fun DiscoverScreenPreview() {
                Surface {
                    DiscoverScreen()
                }
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 6,
            col = 5,
            detail = NoStyleWrapperInPreviewRule.errorMessage("Surface", "DiscoverScreenPreview"),
        )
    }

    @Test
    fun `flags MaterialTheme wrapper inside Preview by default`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.discover.ui

            @Preview
            @Composable
            private fun DiscoverScreenPreview() {
                MaterialTheme {
                    DiscoverScreen()
                }
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 6,
            col = 5,
            detail = NoStyleWrapperInPreviewRule.errorMessage("MaterialTheme", "DiscoverScreenPreview"),
        )
    }

    @Test
    fun `does not flag Preview without wrapper`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.discover.ui

            @Preview
            @Composable
            private fun DiscoverScreenPreview() {
                DiscoverScreen()
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag wrapper in non-Preview function`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.settings.ui

            @Test
            fun testSettingsScreen() {
                TvManiacBackground {
                    SettingsScreen()
                }
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `does not flag wrapper inside production composable`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.discover.ui

            @Composable
            fun DiscoverScreen() {
                TvManiacTheme {
                    DiscoverScreenContent()
                }
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `flags wrapper nested inside Preview body block`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.discover.ui

            @Preview
            @Composable
            private fun NestedWrapperPreview() {
                Column {
                    TvManiacTheme {
                        DiscoverScreen()
                    }
                }
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 7,
            col = 9,
            detail = NoStyleWrapperInPreviewRule.errorMessage("TvManiacTheme", "NestedWrapperPreview"),
        )
    }

    @Test
    fun `does not flag unrelated composable calls inside Preview`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.discover.ui

            @Preview
            @Composable
            private fun DiscoverScreenPreview() {
                Column {
                    Text("Hello")
                }
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun `flags custom wrapper given editorconfig override adds it`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.discover.ui

            @Preview
            @Composable
            private fun DiscoverScreenPreview() {
                MyTheme {
                    DiscoverScreen()
                }
            }
            """.trimIndent(),
        )
            .withEditorConfigOverride(PREVIEW_WRAPPERS_PROPERTY to "MyTheme")
            .hasLintViolationWithoutAutoCorrect(
                line = 6,
                col = 5,
                detail = NoStyleWrapperInPreviewRule.errorMessage("MyTheme", "DiscoverScreenPreview"),
            )
    }

    @Test
    fun `flags wrapper resolved via package prefix override`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.discover.ui

            import com.example.theme.RenamedTheme

            @Preview
            @Composable
            private fun DiscoverScreenPreview() {
                RenamedTheme {
                    DiscoverScreen()
                }
            }
            """.trimIndent(),
        )
            .withEditorConfigOverride(PREVIEW_WRAPPERS_PROPERTY to "")
            .withEditorConfigOverride(PREVIEW_WRAPPER_PACKAGES_PROPERTY to "com.example.theme")
            .hasLintViolationWithoutAutoCorrect(
                line = 8,
                col = 5,
                detail = NoStyleWrapperInPreviewRule.errorMessage("RenamedTheme", "DiscoverScreenPreview"),
            )
    }

    @Test
    fun `does not flag any wrapper given empty wrappers override and no packages`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.discover.ui

            @Preview
            @Composable
            private fun DiscoverScreenPreview() {
                TvManiacTheme {
                    DiscoverScreen()
                }
            }
            """.trimIndent(),
        )
            .withEditorConfigOverride(PREVIEW_WRAPPERS_PROPERTY to "")
            .hasNoLintViolations()
    }

    @Test
    fun `does not flag wrapper resolved via star import even with package prefix override`() {
        assertThat(
            // language=kotlin
            """
            package com.thomaskioko.tvmaniac.discover.ui

            import com.example.theme.*

            @Preview
            @Composable
            private fun DiscoverScreenPreview() {
                RenamedTheme {
                    DiscoverScreen()
                }
            }
            """.trimIndent(),
        )
            .withEditorConfigOverride(PREVIEW_WRAPPERS_PROPERTY to "")
            .withEditorConfigOverride(PREVIEW_WRAPPER_PACKAGES_PROPERTY to "com.example.theme")
            .hasNoLintViolations()
    }
}
