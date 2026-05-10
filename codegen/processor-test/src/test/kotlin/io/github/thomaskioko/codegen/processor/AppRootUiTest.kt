package io.github.thomaskioko.codegen.processor

import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class AppRootUiTest {

    @Test
    fun `should generate AppRootProvider and extension for AppRootUi composable`() {
        val sources = TestStubs.appRootUiStubs.toMap() + mapOf(
            "RootPresenter.kt" to """
                package com.thomaskioko.tvmaniac.presenter.root

                public interface RootPresenter
            """.trimIndent(),
            "RootScreen.kt" to """
                package com.thomaskioko.tvmaniac.app.ui

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import com.thomaskioko.tvmaniac.core.base.ActivityScope
                import com.thomaskioko.tvmaniac.navigation.ui.ScreenContent
                import com.thomaskioko.tvmaniac.navigation.ui.SheetContent
                import com.thomaskioko.tvmaniac.presenter.root.RootPresenter
                import io.github.thomaskioko.codegen.annotations.AppRootUi

                @AppRootUi(presenter = RootPresenter::class, parentScope = ActivityScope::class)
                @Composable
                public fun RootScreen(
                    rootPresenter: RootPresenter,
                    screenContents: Set<ScreenContent>,
                    sheetContents: Set<SheetContent>,
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
            setOf("RootScreenAppRootUiBinding.kt"),
            files.keys,
        )

        GoldenFileAssert.assertMatches(
            "app-root-ui",
            "RootScreenAppRootUiBinding.kt",
            files.getValue("RootScreenAppRootUiBinding.kt"),
        )
    }

    @Test
    fun `should fail when AppRootUi has no non-modifier parameters`() {
        val sources = TestStubs.appRootUiStubs.toMap() + mapOf(
            "RootPresenter.kt" to """
                package com.thomaskioko.tvmaniac.presenter.root

                public interface RootPresenter
            """.trimIndent(),
            "RootScreen.kt" to """
                package com.thomaskioko.tvmaniac.app.ui

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import com.thomaskioko.tvmaniac.core.base.ActivityScope
                import com.thomaskioko.tvmaniac.presenter.root.RootPresenter
                import io.github.thomaskioko.codegen.annotations.AppRootUi

                @AppRootUi(presenter = RootPresenter::class, parentScope = ActivityScope::class)
                @Composable
                public fun RootScreen(modifier: Modifier = Modifier) {
                }
            """.trimIndent(),
        )

        val result = ProcessorTestRunner().run(sources)
        assertEquals(
            "Compilation should have failed:\n${result.messages}",
            KotlinCompilation.ExitCode.COMPILATION_ERROR,
            result.exitCode,
        )
        assertTrue(
            "Expected error about missing non-modifier parameter. Messages were:\n${result.messages}",
            result.messages.contains("at least one"),
        )
    }

    @Test
    fun `should fail when first parameter type does not match presenter argument`() {
        val sources = TestStubs.appRootUiStubs.toMap() + mapOf(
            "RootPresenter.kt" to """
                package com.thomaskioko.tvmaniac.presenter.root

                public interface RootPresenter
            """.trimIndent(),
            "OtherPresenter.kt" to """
                package com.thomaskioko.tvmaniac.presenter.root

                public class OtherPresenter
            """.trimIndent(),
            "RootScreen.kt" to """
                package com.thomaskioko.tvmaniac.app.ui

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import com.thomaskioko.tvmaniac.core.base.ActivityScope
                import com.thomaskioko.tvmaniac.presenter.root.OtherPresenter
                import com.thomaskioko.tvmaniac.presenter.root.RootPresenter
                import io.github.thomaskioko.codegen.annotations.AppRootUi

                @AppRootUi(presenter = RootPresenter::class, parentScope = ActivityScope::class)
                @Composable
                public fun RootScreen(
                    other: OtherPresenter,
                    modifier: Modifier = Modifier,
                ) {
                }
            """.trimIndent(),
        )

        val result = ProcessorTestRunner().run(sources)
        assertEquals(
            "Compilation should have failed:\n${result.messages}",
            KotlinCompilation.ExitCode.COMPILATION_ERROR,
            result.exitCode,
        )
        assertTrue(
            "Expected error about parameter type mismatch. Messages were:\n${result.messages}",
            result.messages.contains("does not match the declared presenter type"),
        )
    }
}
