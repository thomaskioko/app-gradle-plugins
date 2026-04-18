package io.github.thomaskioko.codegen.processor.codegen

import com.squareup.kotlinpoet.FileSpec
import io.github.thomaskioko.codegen.processor.data.NavData
import io.github.thomaskioko.codegen.processor.data.ScreenData
import io.github.thomaskioko.codegen.processor.data.SheetData
import io.github.thomaskioko.codegen.processor.data.TabData

internal object FileGenerator {
    fun generate(data: NavData): List<FileSpec> = when (data) {
        is ScreenData -> listOf(
            ScreenGraphGenerator.generate(data),
            NavDestinationBindingGenerator.generate(data),
        )
        is TabData -> listOf(
            ScreenGraphGenerator.generate(data),
            TabDestinationBindingGenerator.generate(data),
        )
        is SheetData -> listOf(
            ScreenGraphGenerator.generate(data),
            SheetDestinationBindingGenerator.generate(data),
        )
    }
}
