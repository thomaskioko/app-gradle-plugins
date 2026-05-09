package io.github.thomaskioko.codegen.processor

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspProcessorOptions
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.File

/**
 * Test harness that runs [NavigationCodegenProcessor] over inline Kotlin source strings and
 * exposes the generated files for golden comparison.
 *
 * Each call to [run] builds a fresh `KotlinCompilation` configured with KSP2, registers the
 * processor as the only symbol processor provider, compiles the supplied sources, and walks the
 * KSP output directory to collect every generated `.kt` file. The result wraps the raw
 * [JvmCompilationResult] (so tests can assert on exit code or messages) alongside a
 * `Map<file name -> contents>` for the generated files.
 *
 * `inheritClassPath = true` lets the compilation see the test module's runtime classpath, which
 * is how it picks up the real `codegen-annotations` jar. The processor reads the actual
 * `@NavDestination` symbol, not a stub.
 */
@OptIn(ExperimentalCompilerApi::class)
internal class ProcessorTestRunner {

    /**
     * The result of one processor run.
     *
     * @property result The raw compilation result. Tests use this to assert on exit code or to
     *   inspect compiler messages (the error path tests assert on these).
     * @property generatedFiles The generated Kotlin files, keyed by simple file name. The
     *   directory layout under KSP's source root is flattened.
     */
    data class RunResult(
        val result: JvmCompilationResult,
        val generatedFiles: Map<String, String>,
    ) {
        val exitCode: KotlinCompilation.ExitCode get() = result.exitCode
        val messages: String get() = result.messages
    }

    /**
     * Compiles [sources] under KSP2 with [NavigationCodegenProcessor] registered, then collects
     * every generated Kotlin file.
     *
     * @param sources The source files to compile, keyed by file name.
     * @return The compilation result and the generated files.
     */
    fun run(sources: Map<String, String>): RunResult {
        val compilation = KotlinCompilation().apply {
            this.sources = sources.map { (name, content) -> SourceFile.kotlin(name, content) }
            useKsp2()
            symbolProcessorProviders = mutableListOf(NavigationCodegenProcessorProvider())
            kspProcessorOptions = mutableMapOf()
            inheritClassPath = true
            messageOutputStream = System.out
        }

        val result = compilation.compile()
        val kspSourcesDir = compilation.kspSourcesDir
        val generated = collectGeneratedKotlinFiles(kspSourcesDir)
        return RunResult(result = result, generatedFiles = generated)
    }

    private fun collectGeneratedKotlinFiles(root: File): Map<String, String> {
        if (!root.exists()) return emptyMap()
        return root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .associate { file -> file.name to file.readText() }
    }
}
