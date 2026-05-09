package io.github.thomaskioko.codegen.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * KSP entry point for the navigation codegen processor. KSP loads this through
 * `META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider` and calls
 * [create] once per build to obtain the [NavigationCodegenProcessor] that will run for that
 * build.
 *
 * Consumer wiring lives in the `useCodegen()` extension on the `scaffold {}` Gradle DSL. See
 * `codegen/docs/get-started.md`. Consumers do not reference this class directly.
 */
public class NavigationCodegenProcessorProvider : SymbolProcessorProvider {
    /**
     * Creates the [NavigationCodegenProcessor] KSP will run for the current build.
     *
     * @param environment KSP's per build environment. Provides the file writer and diagnostic
     *   logger the processor needs.
     * @return A new processor instance bound to the supplied environment.
     */
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        NavigationCodegenProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
        )
}
