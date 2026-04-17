package io.github.thomaskioko.codegen.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

public class NavigationCodegenProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        NavigationCodegenProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
        )
}
