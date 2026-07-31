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
     * @param hideFromObjC Whether to hide the generated types from the Objective-C API. True when
     *   the compilation has a non-JVM target.
     * @return A pair of [FileSpec] outputs: the graph file and the binding file.
     */
    fun generate(data: NavData, hideFromObjC: Boolean): List<FileSpec> = when (data) {
        is ScreenData -> listOf(
            ScreenGraphGenerator.generate(data, hideFromObjC),
            NavDestinationBindingGenerator.generate(data, hideFromObjC),
        )

        is TabData -> listOf(
            ScreenGraphGenerator.generate(data, hideFromObjC),
            TabDestinationBindingGenerator.generate(data, hideFromObjC),
        )
    }
}
