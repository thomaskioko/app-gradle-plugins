package io.github.thomaskioko.codegen.processor.codegen

import com.squareup.kotlinpoet.FileSpec
import io.github.thomaskioko.codegen.processor.data.NavData
import io.github.thomaskioko.codegen.processor.data.ScreenData
import io.github.thomaskioko.codegen.processor.data.TabData

/**
 * Routes each parsed presenter annotation to the code generators that emit its files.
 *
 * Both [ScreenData] and [TabData] reuse [ScreenGraphGenerator] for the graph file because screens
 * and tabs produce the same `@GraphExtension` structure. They differ only in which destination
 * binding generator runs alongside: [NavDestinationBindingGenerator] for screens and overlays,
 * [TabDestinationBindingGenerator] for tab roots.
 *
 * Output is always a list of two [FileSpec] outputs (one graph, one binding) that the caller
 * writes to disk together. They reference each other (the binding takes the graph's `Factory` as
 * a constructor parameter), so neither is meaningful on its own.
 */
internal object FileGenerator {
    /**
     * Generates the graph and binding files for one parsed presenter annotation.
     *
     * @param data The parsed annotation. [ScreenData] for screens and overlays, [TabData] for tab
     *   roots.
     * @return A pair of [FileSpec] outputs: the graph file and the binding file.
     */
    fun generate(data: NavData): List<FileSpec> = when (data) {
        is ScreenData -> listOf(
            ScreenGraphGenerator.generate(data),
            NavDestinationBindingGenerator.generate(data),
        )

        is TabData -> listOf(
            ScreenGraphGenerator.generate(data),
            TabDestinationBindingGenerator.generate(data),
        )
    }
}
