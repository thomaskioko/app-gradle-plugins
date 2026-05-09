package io.github.thomaskioko.codegen.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.writeTo
import io.github.thomaskioko.codegen.processor.codegen.FileGenerator
import io.github.thomaskioko.codegen.processor.codegen.UiBindingGenerator
import io.github.thomaskioko.codegen.processor.data.NavData
import io.github.thomaskioko.codegen.processor.data.UiBindingKind
import io.github.thomaskioko.codegen.processor.parser.parseNavDestinationData
import io.github.thomaskioko.codegen.processor.parser.parseUiBindingData

/**
 * The KSP processor that turns navigation codegen annotations into Metro graph extensions and
 * navigation bindings. Loaded once per build by KSP through [NavigationCodegenProcessorProvider].
 *
 * The processor walks three annotation queries in order: `@NavDestination` (presenter classes),
 * `@ScreenUi`, and `@SheetUi` (composable functions). Each match goes through a parser to produce
 * a typed intermediate representation, then through a generator to produce one or more
 * [FileSpec] outputs that KSP writes to disk. The full pipeline and the rationale behind each
 * step are documented in `codegen/docs/architecture/pipeline.md`.
 *
 * The processor never throws on bad input. Every validation failure is reported through
 * [KSPLogger.error] pinned to the offending symbol, and KSP turns that into a compile error at
 * the source position.
 *
 * @property codeGenerator KSP's file writer. The processor hands every produced [FileSpec] to it
 *   so KSP can place the generated source in the right output directory and track it for
 *   incremental compilation.
 * @property logger KSP's diagnostic sink. The processor uses it to report validation errors that
 *   KSP turns into compile errors at the offending symbol.
 */
public class NavigationCodegenProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    /**
     * Runs one KSP round. Iterates the three annotations the processor knows about, hands each
     * symbol to the matching parser and generator, and returns an empty list of deferred symbols
     * because this processor produces nothing on subsequent rounds.
     *
     * @param resolver KSP's lookup interface for the current round. Provides access to all
     *   annotated symbols visible to the compilation.
     * @return An empty list. The processor does not defer work to later rounds.
     */
    override fun process(resolver: Resolver): List<KSAnnotated> {
        processAnnotation(resolver, Constants.NAV_DESTINATION_FQN, Constants.NAV_DESTINATION) { presenter ->
            parseNavDestinationData(presenter, logger)
        }
        processUiBinding(resolver, Constants.SCREEN_UI_FQN, Constants.SCREEN_UI, UiBindingKind.Screen)
        processUiBinding(resolver, Constants.SHEET_UI_FQN, Constants.SHEET_UI, UiBindingKind.Sheet)
        return emptyList()
    }

    /**
     * Processes one function target annotation (`@ScreenUi` or `@SheetUi`). For each matching
     * function declaration, the parser produces a [io.github.thomaskioko.codegen.processor.data.UiBindingData]
     * and the [UiBindingGenerator] produces a single binding file that KSP writes to disk. A
     * symbol that is not a function is reported as a compile error at its declaration site.
     *
     * @param resolver KSP's lookup interface for the current round.
     * @param fqn Fully qualified name of the annotation to process.
     * @param shortName Short name of the annotation, used in error messages so the user sees the
     *   familiar `@ScreenUi` rather than a fully qualified path.
     * @param kind Whether the annotation marks a screen renderer or an overlay renderer. Picks
     *   which content type and destination cast the generator emits.
     */
    private fun processUiBinding(
        resolver: Resolver,
        fqn: String,
        shortName: String,
        kind: UiBindingKind,
    ) {
        for (symbol in resolver.getSymbolsWithAnnotation(fqn)) {
            val function = symbol as? KSFunctionDeclaration ?: run {
                logger.error("@$shortName can only be applied to functions", symbol)
                continue
            }
            val data = parseUiBindingData(function, kind, logger) ?: continue
            writeFunctionFiles(function, listOf(UiBindingGenerator.generate(data)))
        }
    }

    /**
     * Processes one class target annotation (currently just `@NavDestination`). For each matching
     * class declaration, the supplied [parse] function produces a [NavData] and [FileGenerator]
     * produces one or more files that KSP writes to disk. A symbol that is not a class is
     * reported as a compile error at its declaration site.
     *
     * @param resolver KSP's lookup interface for the current round.
     * @param fqn Fully qualified name of the annotation to process.
     * @param shortName Short name of the annotation, used in error messages.
     * @param parse Function that turns a class declaration into a [NavData], or returns `null`
     *   after logging a validation error to skip the symbol.
     */
    private fun processAnnotation(
        resolver: Resolver,
        fqn: String,
        shortName: String,
        parse: (KSClassDeclaration) -> NavData?,
    ) {
        for (symbol in resolver.getSymbolsWithAnnotation(fqn)) {
            val declaration = symbol as? KSClassDeclaration ?: run {
                logger.error("@$shortName can only be applied to classes", symbol)
                continue
            }
            val data = parse(declaration) ?: continue
            writeFiles(declaration, FileGenerator.generate(data))
        }
    }

    /**
     * Writes the generated files for a class target annotation. Uses KSP's incremental flag
     * `aggregating = false` so the output depends only on the source file the annotation lives
     * in. Editing one feature does not invalidate sibling features.
     *
     * If the source's containing file cannot be resolved (typically because it was synthesised by
     * another processor in the same round), the symbol is skipped with a warning rather than
     * raising an error.
     *
     * @param source The presenter class the annotation was attached to. Used to look up the
     *   containing source file for the incremental dependency hint.
     * @param files The generated files to write. Typically two: a graph and a binding.
     */
    private fun writeFiles(source: KSClassDeclaration, files: List<FileSpec>) {
        val containingFile = source.containingFile ?: run {
            logger.warn("Cannot determine containing file for ${source.qualifiedName?.asString()}", source)
            return
        }
        val deps = Dependencies(aggregating = false, containingFile)
        for (file in files) {
            file.writeTo(codeGenerator, deps)
        }
    }

    /**
     * Function target equivalent of [writeFiles]. Same KSP incremental semantics.
     *
     * @param source The composable function the annotation was attached to. Used to look up the
     *   containing source file for the incremental dependency hint.
     * @param files The generated files to write. Always one for `@ScreenUi` and `@SheetUi`.
     */
    private fun writeFunctionFiles(source: KSFunctionDeclaration, files: List<FileSpec>) {
        val containingFile = source.containingFile ?: run {
            logger.warn("Cannot determine containing file for ${source.qualifiedName?.asString()}", source)
            return
        }
        val deps = Dependencies(aggregating = false, containingFile)
        for (file in files) {
            file.writeTo(codeGenerator, deps)
        }
    }
}
