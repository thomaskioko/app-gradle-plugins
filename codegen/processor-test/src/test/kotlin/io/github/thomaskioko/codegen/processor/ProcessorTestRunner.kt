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
 * Runs the [NavigationCodegenProcessor] over a set of source strings and exposes the generated
 * Kotlin sources alongside the raw [JvmCompilationResult] for assertion.
 */
@OptIn(ExperimentalCompilerApi::class)
internal class ProcessorTestRunner {

    data class RunResult(
        val result: JvmCompilationResult,
        val generatedFiles: Map<String, String>,
    ) {
        val exitCode: KotlinCompilation.ExitCode get() = result.exitCode
        val messages: String get() = result.messages
    }

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
