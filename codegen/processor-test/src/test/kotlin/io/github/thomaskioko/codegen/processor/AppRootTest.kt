package io.github.thomaskioko.codegen.processor

import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class AppRootTest {

    @Test
    fun `should generate binding container for AppRoot annotated implementation`() {
        val sources = TestStubs.baseStubs.toMap() + mapOf(
            "RootPresenter.kt" to """
                package com.thomaskioko.tvmaniac.presenter.root

                public interface RootPresenter
            """.trimIndent(),
            "DefaultRootPresenter.kt" to """
                package com.thomaskioko.tvmaniac.presenter.root

                import com.arkivanov.decompose.ComponentContext
                import com.thomaskioko.tvmaniac.core.base.ActivityScope
                import dev.zacsweers.metro.Assisted
                import dev.zacsweers.metro.AssistedFactory
                import dev.zacsweers.metro.AssistedInject
                import io.github.thomaskioko.codegen.annotations.AppRoot

                @AppRoot(parentScope = ActivityScope::class)
                @AssistedInject
                public class DefaultRootPresenter(
                    @Assisted componentContext: ComponentContext,
                ) : RootPresenter, ComponentContext by componentContext {

                    @AssistedFactory
                    public fun interface Factory {
                        public fun create(componentContext: ComponentContext): DefaultRootPresenter
                    }
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
            setOf("RootPresenterBindingContainer.kt"),
            files.keys,
        )

        GoldenFileAssert.assertMatches(
            "app-root",
            "RootPresenterBindingContainer.kt",
            files.getValue("RootPresenterBindingContainer.kt"),
        )
    }

    @Test
    fun `should fail when AppRoot is missing AssistedInject`() {
        val sources = TestStubs.baseStubs.toMap() + mapOf(
            "RootPresenter.kt" to """
                package com.thomaskioko.tvmaniac.presenter.root

                public interface RootPresenter
            """.trimIndent(),
            "DefaultRootPresenter.kt" to """
                package com.thomaskioko.tvmaniac.presenter.root

                import com.arkivanov.decompose.ComponentContext
                import com.thomaskioko.tvmaniac.core.base.ActivityScope
                import io.github.thomaskioko.codegen.annotations.AppRoot

                @AppRoot(parentScope = ActivityScope::class)
                public class DefaultRootPresenter(
                    componentContext: ComponentContext,
                ) : RootPresenter, ComponentContext by componentContext
            """.trimIndent(),
        )

        val result = ProcessorTestRunner().run(sources)
        assertEquals(
            "Compilation should have failed:\n${result.messages}",
            KotlinCompilation.ExitCode.COMPILATION_ERROR,
            result.exitCode,
        )
        assertTrue(
            "Expected error about missing @AssistedInject. Messages were:\n${result.messages}",
            result.messages.contains("requires @AssistedInject"),
        )
    }

    @Test
    fun `should fail when AppRoot is missing nested AssistedFactory`() {
        val sources = TestStubs.baseStubs.toMap() + mapOf(
            "RootPresenter.kt" to """
                package com.thomaskioko.tvmaniac.presenter.root

                public interface RootPresenter
            """.trimIndent(),
            "DefaultRootPresenter.kt" to """
                package com.thomaskioko.tvmaniac.presenter.root

                import com.arkivanov.decompose.ComponentContext
                import com.thomaskioko.tvmaniac.core.base.ActivityScope
                import dev.zacsweers.metro.Assisted
                import dev.zacsweers.metro.AssistedInject
                import io.github.thomaskioko.codegen.annotations.AppRoot

                @AppRoot(parentScope = ActivityScope::class)
                @AssistedInject
                public class DefaultRootPresenter(
                    @Assisted componentContext: ComponentContext,
                ) : RootPresenter, ComponentContext by componentContext
            """.trimIndent(),
        )

        val result = ProcessorTestRunner().run(sources)
        assertEquals(
            "Compilation should have failed:\n${result.messages}",
            KotlinCompilation.ExitCode.COMPILATION_ERROR,
            result.exitCode,
        )
        assertTrue(
            "Expected error about missing nested @AssistedFactory. Messages were:\n${result.messages}",
            result.messages.contains("requires a nested @AssistedFactory"),
        )
    }

    @Test
    fun `should fail when AppRoot implementation has no bound interface`() {
        val sources = TestStubs.baseStubs.toMap() + mapOf(
            "DefaultRootPresenter.kt" to """
                package com.thomaskioko.tvmaniac.presenter.root

                import com.arkivanov.decompose.ComponentContext
                import com.thomaskioko.tvmaniac.core.base.ActivityScope
                import dev.zacsweers.metro.Assisted
                import dev.zacsweers.metro.AssistedFactory
                import dev.zacsweers.metro.AssistedInject
                import io.github.thomaskioko.codegen.annotations.AppRoot

                @AppRoot(parentScope = ActivityScope::class)
                @AssistedInject
                public class DefaultRootPresenter(
                    @Assisted componentContext: ComponentContext,
                ) : ComponentContext by componentContext {

                    @AssistedFactory
                    public fun interface Factory {
                        public fun create(componentContext: ComponentContext): DefaultRootPresenter
                    }
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
            "Expected error about missing bound interface. Messages were:\n${result.messages}",
            result.messages.contains("requires the implementation to extend exactly one"),
        )
    }
}
