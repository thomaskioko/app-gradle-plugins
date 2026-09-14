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
package io.github.thomaskioko.gradle.tasks

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

@CacheableTask
internal abstract class MokoResourceGeneratorTask
@Inject constructor(
    objectFactory: ObjectFactory,
    layout: ProjectLayout,
) : DefaultTask() {

    init {
        description = "Generates resource sealed classes from Moko resources"
        group = "build"
    }

    @get:Input
    internal val resourcePackage: Property<String> = objectFactory.property(String::class.java)
        .convention("com.thomaskioko.tvmaniac.i18n")

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal val mokoGeneratedDir: DirectoryProperty = objectFactory.directoryProperty()
        .convention(
            resourcePackage.flatMap { pkg ->
                val packagePath = pkg.replace('.', '/')
                layout.buildDirectory.dir("generated/moko-resources/commonMain/src/$packagePath")
            },
        )

    @get:OutputDirectory
    internal val commonMainOutput: DirectoryProperty = objectFactory.directoryProperty()
        .convention(layout.buildDirectory.dir("generated/resources"))

    @TaskAction
    internal fun generate() {
        val outputDir = commonMainOutput.get().asFile
        val sourceDir = mokoGeneratedDir.get().asFile
        val packageName = resourcePackage.get()
        val sources = sourceDir.listFiles { file -> file.extension == "kt" }.orEmpty().toList()

        if (sources.isEmpty()) {
            logger.warn("No Moko generated sources found in ${sourceDir.absolutePath}")
            return
        }

        outputDir.deleteRecursively()
        outputDir.mkdirs()

        val mrClass = ClassName(packageName, "MR")
        val (stringKeys, pluralKeys) = readKeys(sources)

        stringResourceKeyFileSpec(
            packageName = packageName,
            stringKeys = stringKeys,
            mrClass = mrClass,
        ).writeTo(outputDir)

        pluralsResourceKeyFileSpec(
            packageName = packageName,
            pluralKeys = pluralKeys,
            mrClass = mrClass,
        ).writeTo(outputDir)
    }

    internal fun readKeys(sources: List<File>): Pair<List<String>, List<String>> {
        val contents = sources.map { it.readText() }
        return keysOf(contents, "strings", "StringResource") to keysOf(contents, "plurals", "PluralsResource")
    }

    private fun keysOf(contents: List<String>, accessor: String, resourceType: String): List<String> {
        val declaration = Regex("""\bval MR\.$accessor\.(\w+): $resourceType\b""")
        return contents.flatMap { content -> declaration.findAll(content).map { it.groupValues[1] } }.sorted()
    }

    internal fun toPascalCase(name: String): String {
        return name.split('_').joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }
    }

    internal fun stringResourceKeyFileSpec(
        packageName: String,
        stringKeys: List<String>,
        mrClass: ClassName,
    ): FileSpec = resourceKeyFileSpec(
        packageName = packageName,
        className = "StringResourceKey",
        resourceTypeName = "StringResource",
        mrAccessor = "strings",
        keys = stringKeys,
        mrClass = mrClass,
    )

    internal fun pluralsResourceKeyFileSpec(
        packageName: String,
        pluralKeys: List<String>,
        mrClass: ClassName,
    ): FileSpec = resourceKeyFileSpec(
        packageName = packageName,
        className = "PluralsResourceKey",
        resourceTypeName = "PluralsResource",
        mrAccessor = "plurals",
        keys = pluralKeys,
        mrClass = mrClass,
    )

    private fun resourceKeyFileSpec(
        packageName: String,
        className: String,
        resourceTypeName: String,
        mrAccessor: String,
        keys: List<String>,
        mrClass: ClassName,
    ): FileSpec {
        val resourceType = ClassName(MOKO_RESOURCES_PACKAGE, resourceTypeName)
        val lazyResourceType = LAZY_CLASS.parameterizedBy(resourceType)

        return FileSpec.builder(packageName, className)
            .addType(
                TypeSpec.classBuilder(className)
                    .addModifiers(KModifier.SEALED)
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter("resourceProvider", lazyResourceType)
                            .build(),
                    )
                    .addProperty(
                        PropertySpec.builder("resourceId", resourceType)
                            .getter(
                                FunSpec.getterBuilder()
                                    .addStatement("return resourceProvider.value")
                                    .build(),
                            )
                            .build(),
                    )
                    .addProperty(
                        PropertySpec.builder("resourceProvider", lazyResourceType)
                            .initializer("resourceProvider")
                            .addModifiers(KModifier.PRIVATE)
                            .build(),
                    )
                    .addTypes(
                        keys.map { key ->
                            TypeSpec.objectBuilder(toPascalCase(key))
                                .addModifiers(KModifier.DATA)
                                .superclass(ClassName(packageName, className))
                                .addSuperclassConstructorParameter("lazy { %T.$mrAccessor.%N }", mrClass, key)
                                .build()
                        },
                    )
                    .build(),
            )
            .build()
    }

    private companion object {
        const val MOKO_RESOURCES_PACKAGE = "dev.icerock.moko.resources"
        val LAZY_CLASS = ClassName("kotlin", "Lazy")
    }
}
