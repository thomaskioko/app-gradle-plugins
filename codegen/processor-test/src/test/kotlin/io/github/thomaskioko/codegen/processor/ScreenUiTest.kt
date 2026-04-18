package io.github.thomaskioko.codegen.processor

import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class ScreenUiTest {

    @Test
    fun `should generate ScreenContent binding for ScreenUi composable`() {
        val sources = TestStubs.uiStubs.toMap() + mapOf(
            "DebugPresenter.kt" to """
                package com.thomaskioko.tvmaniac.debug.presenter

                public class DebugPresenter
            """.trimIndent(),
            "DebugMenuScreen.kt" to """
                package com.thomaskioko.tvmaniac.debug.ui

                import androidx.compose.ui.Composable
                import androidx.compose.ui.Modifier
                import com.thomaskioko.tvmaniac.core.base.ActivityScope
                import com.thomaskioko.tvmaniac.debug.presenter.DebugPresenter
                import io.github.thomaskioko.codegen.annotations.ScreenUi

                @ScreenUi(presenter = DebugPresenter::class, parentScope = ActivityScope::class)
                @Composable
                public fun DebugMenuScreen(
                    presenter: DebugPresenter,
                    modifier: Modifier = Modifier,
                ) {
                }
            """.trimIndent(),
        )

        val result = ProcessorTestRunner().run(sources)
        assertEquals(
            "Compilation failed:\n${result.messages}",
            KotlinCompilation.ExitCode.OK,
            result.exitCode,
        )

        val files = result.generatedFiles
        assertEquals(
            "Expected exactly 1 generated file, got ${files.keys}",
            setOf("DebugMenuScreenUiBinding.kt"),
            files.keys,
        )

        GoldenFileAssert.assertMatches(
            "screen-ui",
            "DebugMenuScreenUiBinding.kt",
            files.getValue("DebugMenuScreenUiBinding.kt"),
        )
    }
}
