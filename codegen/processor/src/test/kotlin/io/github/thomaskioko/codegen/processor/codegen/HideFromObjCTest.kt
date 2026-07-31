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

import com.squareup.kotlinpoet.ClassName
import io.github.thomaskioko.codegen.processor.data.AppRootData
import io.github.thomaskioko.codegen.processor.data.ChildPresenterData
import io.github.thomaskioko.codegen.processor.data.ScreenData
import io.github.thomaskioko.codegen.processor.data.TabData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the `hideFromObjC` flag on every generator. The compile testing suite in
 * `processor-test` runs JVM-only compilations, where the flag is always false, so the true path
 * is asserted here on the rendered [com.squareup.kotlinpoet.FileSpec] text instead.
 */
class HideFromObjCTest {

    @Test
    fun `should hide the child graph and its factory given a non jvm target`() {
        val file = ChildGraphGenerator.generate(childPresenterData(), hideFromObjC = true).toString()

        assertEquals(2, file.occurrencesOf("@HiddenFromObjC"))
        assertTrue(file.contains("@file:OptIn(ExperimentalObjCRefinement::class)"))
    }

    @Test
    fun `should hide the screen graph and its factory given a non jvm target`() {
        val file = ScreenGraphGenerator.generate(screenData(), hideFromObjC = true).toString()

        assertEquals(2, file.occurrencesOf("@HiddenFromObjC"))
        assertTrue(file.contains("@file:OptIn(ExperimentalObjCRefinement::class)"))
    }

    @Test
    fun `should hide the destination binding and its companion given a non jvm target`() {
        val screenBinding = NavDestinationBindingGenerator.generate(screenData(), hideFromObjC = true).toString()
        val tabBinding = TabDestinationBindingGenerator.generate(tabData(), hideFromObjC = true).toString()

        assertEquals(2, screenBinding.occurrencesOf("@HiddenFromObjC"))
        assertEquals(2, tabBinding.occurrencesOf("@HiddenFromObjC"))
        assertTrue(screenBinding.contains("@file:OptIn(ExperimentalObjCRefinement::class)"))
        assertTrue(tabBinding.contains("@file:OptIn(ExperimentalObjCRefinement::class)"))
    }

    @Test
    fun `should hide the app root binding container given a non jvm target`() {
        val file = AppRootBindingGenerator.generate(appRootData(), hideFromObjC = true).toString()

        assertEquals(1, file.occurrencesOf("@HiddenFromObjC"))
        assertTrue(file.contains("@file:OptIn(ExperimentalObjCRefinement::class)"))
    }

    @Test
    fun `should emit no refinement annotations given a jvm only target`() {
        val files = listOf(
            ChildGraphGenerator.generate(childPresenterData(), hideFromObjC = false),
            ScreenGraphGenerator.generate(screenData(), hideFromObjC = false),
            NavDestinationBindingGenerator.generate(screenData(), hideFromObjC = false),
            TabDestinationBindingGenerator.generate(tabData(), hideFromObjC = false),
            AppRootBindingGenerator.generate(appRootData(), hideFromObjC = false),
        )

        files.map { it.toString() }.forEach { file ->
            assertFalse(file.contains("HiddenFromObjC"))
            assertFalse(file.contains("ExperimentalObjCRefinement"))
        }
    }

    private fun childPresenterData() = ChildPresenterData(
        presenterClass = ClassName("com.example.shows", "ShowsPresenter"),
        baseName = "Shows",
        packageName = "com.example.shows.di",
        scope = ClassName("com.example.shows", "ShowsScope"),
        parentScope = ClassName("com.example", "ActivityScope"),
    )

    private fun screenData() = ScreenData(
        presenterClass = ClassName("com.example.shows", "ShowsPresenter"),
        baseName = "Shows",
        packageName = "com.example.shows.di",
        parentScope = ClassName("com.example", "ActivityScope"),
        scope = ClassName("com.example.shows", "ShowsRoute"),
        route = ClassName("com.example.shows", "ShowsRoute"),
    )

    private fun tabData() = TabData(
        presenterClass = ClassName("com.example.shows", "ShowsPresenter"),
        baseName = "Shows",
        packageName = "com.example.shows.di",
        parentScope = ClassName("com.example", "ActivityScope"),
        scope = ClassName("com.example.shows", "ShowsRoute"),
        configEnclosing = ClassName("com.example.shows", "ShowsRoute"),
    )

    private fun appRootData() = AppRootData(
        implClassName = ClassName("com.example.root", "DefaultRootPresenter"),
        interfaceClassName = ClassName("com.example.root", "RootPresenter"),
        factoryClassName = ClassName("com.example.root", "DefaultRootPresenter", "Factory"),
        factoryFunctionName = "create",
        parentScope = ClassName("com.example", "ActivityScope"),
        packageName = "com.example.root.di",
    )

    private fun String.occurrencesOf(needle: String): Int = split(needle).size - 1
}
