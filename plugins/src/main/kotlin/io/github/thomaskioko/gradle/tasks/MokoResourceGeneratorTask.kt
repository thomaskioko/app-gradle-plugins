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
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

@CacheableTask
public abstract class MokoResourceGeneratorTask
@Inject constructor(
    objectFactory: ObjectFactory,
    layout: ProjectLayout,
) : DefaultTask() {

    init {
        description = "Generates resource sealed classes from Moko resources"
        group = "build"
    }

    @get:Input
    public val resourcePackage: Property<String> = objectFactory.property(String::class.java)
        .convention("com.thomaskioko.tvmaniac.i18n")

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public val mokoGeneratedFile: RegularFileProperty = objectFactory.fileProperty()
        .convention(
            resourcePackage.flatMap { pkg ->
                val packagePath = pkg.replace('.', '/')
                layout.buildDirectory.file("generated/moko-resources/commonMain/src/$packagePath/MR.kt")
            },
        )

    @get:OutputDirectory
    public val commonMainOutput: DirectoryProperty = objectFactory.directoryProperty()
        .convention(layout.buildDirectory.dir("generated/resources"))

    @TaskAction
    public fun generate() {
        val outputDir = commonMainOutput.get().asFile
        val mrFile = mokoGeneratedFile.get().asFile
        val packageName = resourcePackage.get()

        if (!mrFile.exists()) {
            logger.warn("MR.kt file not found at ${mrFile.absolutePath}")
            return
        }

        outputDir.deleteRecursively()
        outputDir.mkdirs()

        val mrClass = ClassName(packageName, "MR")
        val (stringKeys, pluralKeys) = readKeysFromMRFile(mrFile)

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

    internal fun readKeysFromMRFile(mrFile: File): Pair<List<String>, List<String>> {
        val stringKeys = mutableListOf<String>()
        val pluralKeys = mutableListOf<String>()
        var isInStringsObject = false
        var isInPluralsObject = false

        mrFile.bufferedReader().use { reader ->
            reader.lineSequence().forEach { line ->
                when {
                    line.contains("object strings") -> {
                        isInStringsObject = true
                        isInPluralsObject = false
                    }

                    line.contains("object plurals") -> {
                        isInStringsObject = false
                        isInPluralsObject = true
                    }

                    line.trim() == "}" -> {
                        isInStringsObject = false
                        isInPluralsObject = false
                    }

                    isInStringsObject && line.contains("public val") && line.contains(": StringResource") -> {
                        extractKeyName(line)?.let { stringKeys.add(it) }
                    }

                    isInPluralsObject && line.contains("public val") && line.contains(": PluralsResource") -> {
                        extractKeyName(line)?.let { pluralKeys.add(it) }
                    }
                }
            }
        }
        return stringKeys to pluralKeys
    }

    private fun extractKeyName(line: String): String? {
        // Example: public val button_error_retry: StringResource
        return line.split("public val")
            .getOrNull(1)
            ?.trim()
            ?.split(":")
            ?.getOrNull(0)
            ?.trim()
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
