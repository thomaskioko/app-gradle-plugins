package io.github.thomaskioko.gradle.plugins.utils

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.variant.AndroidComponentsExtension
import io.github.thomaskioko.gradle.plugins.extensions.AndroidExtension
import io.github.thomaskioko.gradle.plugins.extensions.BaseExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

internal val Project.baseExtension: BaseExtension
    get() = extensions.getByType(BaseExtension::class.java)

internal val Project.androidExtension: AndroidExtension
    get() = baseExtension.extensions.getByType(AndroidExtension::class.java)

/**
 * This function provides a convenient way to configure the Kotlin Multiplatform plugin using a lambda expression.
 */
internal fun Project.kotlinMultiplatform(block: KotlinMultiplatformExtension.() -> Unit) {
    extensions.configure(KotlinMultiplatformExtension::class.java) {
        it.block()
    }
}

/**
 * This function provides a convenient way to access and modify the settings of the Compose Compiler
 * plugin within a Gradle project.
 */
internal fun Project.composeCompiler(block: ComposeCompilerGradlePluginExtension.() -> Unit) {
    extensions.configure(ComposeCompilerGradlePluginExtension::class.java) {
        it.block()
    }
}

/**
 * Configures the Android Gradle Plugin for the [Project] by applying the specified [block].
 *
 * This method allows modification of properties and settings within the Android Gradle Plugin's
 * [CommonExtension] configuration block.
 *
 * @param block A lambda receiver of type [CommonExtension], used to perform configurations
 * applied to the Android Gradle Plugin for the project.
 */
internal fun Project.android(block: CommonExtension<*, *, *, *, *, *>.() -> Unit) {
    extensions.configure(CommonExtension::class.java) {
        it.block()
    }
}

/**
 * This function simplifies the process of configuring an Android application module by providing a type-safe builder pattern.
 */
internal fun Project.androidApp(block: ApplicationExtension.() -> Unit) {
    extensions.configure(ApplicationExtension::class.java) {
        it.block()
    }
}

/**
 * This function provides a concise way to customize the Android build process, including variant configuration and artifact management.
 */
internal fun Project.androidComponents(block: AndroidComponentsExtension<*, *, *>.() -> Unit) {
    extensions.configure(AndroidComponentsExtension::class.java) {
        it.block()
    }
}

/**
 * Configures Kotlin-related settings for the current project by applying the provided `block`
 * on the project's Kotlin extension (`KotlinProjectExtension`).
 *
 * @param block A lambda used to configure the `KotlinProjectExtension`.
 */
internal fun Project.kotlin(block: KotlinProjectExtension.() -> Unit) {
    (project.extensions.getByName("kotlin") as KotlinProjectExtension).block()
}

/**
 * Configures the `JavaPluginExtension` for the given project using the provided configuration block.
 *
 * @param block A lambda expression of type `JavaPluginExtension.() -> Unit` that defines the configuration logic
 *              for the `JavaPluginExtension` in the context of the current project.
 */
internal fun Project.java(block: JavaPluginExtension.() -> Unit) {
    extensions.configure(JavaPluginExtension::class.java) {
        it.block()
    }
}

/**
 * Configures the compiler options for a Kotlin project extension.
 * This method handles different types of Kotlin project extensions, such as JVM, Android, or Multiplatform,
 * and applies the specified configurations to their respective compiler options.
 *
 * @param configure A lambda expression used to configure the Kotlin compiler options.
 */
internal fun KotlinProjectExtension.compilerOptions(configure: KotlinCommonCompilerOptions.() -> Unit) {
    when (this) {
        is KotlinJvmProjectExtension -> compilerOptions(configure)
        is KotlinAndroidProjectExtension -> compilerOptions(configure)
        is KotlinMultiplatformExtension -> {
            compilerOptions(configure)
            targets.configureEach { target ->
                (target as? HasConfigurableKotlinCompilerOptions<*>)?.compilerOptions(configure)
            }
        }

        else -> throw IllegalStateException("Unsupported kotlin extension ${this::class}")
    }
}

internal fun KotlinMultiplatformAndroidLibraryTarget.jvmCompilerOptions(block: KotlinJvmCompilerOptions.() -> Unit) {
    compilations.configureEach { compilation ->
        compilation.compileTaskProvider.configure {
            compilerOptions {
                block()
            }
        }
    }
}

/**
 * Provides the package name defined in the project's Gradle properties as a [Provider].
 * The method retrieves the value of the "package.name" property and ensures it is not blank.
 * If the property is missing or blank, an error is thrown, informing the user to define it.
 *
 * @return A [Provider] of [String] representing the package name.
 * Throws an error if the package name is missing or empty in the Gradle properties.
 */
internal fun Project.getPackageNameProvider(): Provider<String> =
    stringProperty("package.name").map {
        it.takeIf { it.isNotBlank() }
            ?: error("Required property 'package.name' is missing or empty in gradle.properties. Add: package.name=com.yourcompany.yourapp")
    }

/**
 * Capitalizes the first character of this string.
 *
 * @return A new string with the first character converted to title case.
 */
internal fun String.capitalizeFirst() = replaceFirstChar { it.titlecase() }
