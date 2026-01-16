package io.github.thomaskioko.gradle.plugins

import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.variant.HasAndroidTestBuilder
import com.android.build.api.variant.HasUnitTestBuilder
import io.github.thomaskioko.gradle.plugins.extensions.AndroidExtension
import io.github.thomaskioko.gradle.plugins.utils.addIfNotNull
import io.github.thomaskioko.gradle.plugins.utils.android
import io.github.thomaskioko.gradle.plugins.utils.androidComponents
import io.github.thomaskioko.gradle.plugins.utils.androidLibrary
import io.github.thomaskioko.gradle.plugins.utils.baseExtension
import io.github.thomaskioko.gradle.plugins.utils.configure
import io.github.thomaskioko.gradle.plugins.utils.defaultTestSetup
import io.github.thomaskioko.gradle.plugins.utils.disableAndroidLibraryTasks
import io.github.thomaskioko.gradle.plugins.utils.getDependencyOrNull
import io.github.thomaskioko.gradle.plugins.utils.getPackageNameProvider
import io.github.thomaskioko.gradle.plugins.utils.getVersion
import io.github.thomaskioko.gradle.plugins.utils.javaTargetVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

public abstract class AndroidPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        if (!target.plugins.hasPlugin("com.android.application")) {
            target.plugins.apply("com.android.library")
        }
        if (!target.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
            target.plugins.apply("org.jetbrains.kotlin.android")
        }
        target.plugins.apply(BasePlugin::class.java)

        target.baseExtension.extensions.create("android", AndroidExtension::class.java)

        target.androidSetup()
        target.configureLint()
        target.configureUnitTests()
        target.disableAndroidTests()
        target.disableAndroidLibraryTasks()
    }

    private fun Project.configureLint() {
        android {
            lint.configure(project)
        }
    }

    @Suppress("UnstableApiUsage")
    private fun Project.configureUnitTests() {
        androidLibrary {
            testOptions {
                unitTests.all(Test::defaultTestSetup)
            }
        }

        androidComponents {
            beforeVariants(
                selector().withBuildType("release"),
            ) {
                (it as? HasUnitTestBuilder)?.enableUnitTest = false
            }
        }
    }

    private fun Project.disableAndroidTests() {
        androidComponents {
            beforeVariants {
                if (it is HasAndroidTestBuilder) {
                    it.androidTest.enable = false
                }
            }
        }
    }
}

internal fun Project.androidSetup() {
    val desugarLibrary = project.getDependencyOrNull("android-desugarJdkLibs")
    android {
        namespace = pathBasedAndroidNamespace()

        compileSdk = getVersion("android-compile").toInt()
        defaultConfig.minSdk = getVersion("android-min").toInt()
        (defaultConfig as? ApplicationDefaultConfig)?.let {
            it.targetSdk = getVersion("android-target").toInt()
        }
    }

    androidLibrary {
        // default all features to false, they will be enabled through AndroidExtension
        buildFeatures {
            viewBinding = false
            resValues = false
            buildConfig = false
            aidl = false
            shaders = false
        }

        compileOptions {
            isCoreLibraryDesugaringEnabled = desugarLibrary != null
            sourceCompatibility = javaTargetVersion.get()
            targetCompatibility = javaTargetVersion.get()
        }
    }

    dependencies.addIfNotNull("coreLibraryDesugaring", desugarLibrary)
}

internal fun Project.pathBasedAndroidNamespace(): String {
    val transformedPath = path.drop(1)
        .split(":")
        .mapIndexed { index, pathElement ->
            val parts = pathElement.split("-")
            if (index == 0) {
                parts.joinToString(separator = ".")
            } else {
                parts.joinToString(separator = "")
            }
        }
        .joinToString(separator = ".")

    return "${getPackageNameProvider().get()}.$transformedPath"
}
