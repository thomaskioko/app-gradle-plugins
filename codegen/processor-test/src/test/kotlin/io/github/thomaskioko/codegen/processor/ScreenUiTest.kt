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
