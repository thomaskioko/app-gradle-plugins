package io.github.thomaskioko.codegen.featureflag

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspProcessorOptions
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import io.github.thomaskioko.codegen.featureflag.processor.FeatureFlagCodegenProcessorProvider
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.File

/**
 * Test harness that runs [io.github.thomaskioko.codegen.featureflag.processor.FeatureFlagCodegenProcessor]
 * over inline Kotlin source strings and exposes the generated files for golden comparison.
 *
 * Mirrors the navigation `ProcessorTestRunner` shape. Each [run] call builds a fresh
 * `KotlinCompilation` configured with KSP2, registers
 * [FeatureFlagCodegenProcessorProvider] as the only symbol processor provider, compiles the
 * supplied sources, and walks the KSP output directory to collect every generated `.kt` file.
 */
@OptIn(ExperimentalCompilerApi::class)
internal class FeatureFlagTestRunner {

    /**
     * The result of one processor run.
     *
     * @property result The raw compilation result. Error-path tests assert on [result.messages]
     *   to verify the diagnostic format.
     * @property generatedFiles The generated Kotlin files keyed by simple file name.
     */
    data class RunResult(
        val result: JvmCompilationResult,
        val generatedFiles: Map<String, String>,
    ) {
        val exitCode: KotlinCompilation.ExitCode get() = result.exitCode
        val messages: String get() = result.messages
    }

    /**
     * Compiles [sources] under KSP2 with [FeatureFlagCodegenProcessor] registered, then collects
     * every generated Kotlin file.
     */
    fun run(sources: Map<String, String>): RunResult {
        val compilation = KotlinCompilation().apply {
            this.sources = sources.map { (name, content) -> SourceFile.kotlin(name, content) }
            useKsp2()
            symbolProcessorProviders = mutableListOf(FeatureFlagCodegenProcessorProvider())
            kspProcessorOptions = mutableMapOf()
            inheritClassPath = true
            messageOutputStream = System.out
        }

        val result = compilation.compile()
        val generated = collectGeneratedKotlinFiles(compilation.kspSourcesDir)
        return RunResult(result = result, generatedFiles = generated)
    }

    private fun collectGeneratedKotlinFiles(root: File): Map<String, String> {
        if (!root.exists()) return emptyMap()
        return root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .associate { file -> file.name to file.readText() }
    }
}
