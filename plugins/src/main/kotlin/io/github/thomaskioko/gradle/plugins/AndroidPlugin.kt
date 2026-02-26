package io.github.thomaskioko.gradle.plugins

import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.dsl.CompileOptions
import com.android.build.api.dsl.TestExtension
import com.android.build.api.dsl.TestOptions
import com.android.build.api.variant.HasAndroidTestBuilder
import com.android.build.api.variant.HasUnitTestBuilder
import io.github.thomaskioko.gradle.plugins.extensions.AndroidExtension
import io.github.thomaskioko.gradle.plugins.utils.addIfNotNull
import io.github.thomaskioko.gradle.plugins.utils.android
import io.github.thomaskioko.gradle.plugins.utils.androidApp
import io.github.thomaskioko.gradle.plugins.utils.androidComponents
import io.github.thomaskioko.gradle.plugins.utils.androidLibrary
import io.github.thomaskioko.gradle.plugins.utils.baseExtension
import io.github.thomaskioko.gradle.plugins.utils.configureCommonAndroid
import io.github.thomaskioko.gradle.plugins.utils.defaultTestSetup
import io.github.thomaskioko.gradle.plugins.utils.disableAndroidLibraryTasks
import io.github.thomaskioko.gradle.plugins.utils.getDependencyOrNull
import io.github.thomaskioko.gradle.plugins.utils.getVersion
import io.github.thomaskioko.gradle.plugins.utils.javaTargetVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

public abstract class AndroidPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val isApplication = target.plugins.hasPlugin("com.android.application")
        if (!isApplication) {
            target.plugins.apply("com.android.library")
        }
        target.plugins.apply(BasePlugin::class.java)
        //TODO:: Move back to base plugin
        target.plugins.apply("com.autonomousapps.dependency-analysis")

        target.baseExtension.extensions.create("android", AndroidExtension::class.java)

        target.androidSetup()
        target.configureUnitTests()
        target.disableAndroidTests()
        if (!isApplication) {
            target.disableAndroidLibraryTasks()
        }
    }

    private fun Project.configureUnitTests() {
        val configureTestResources: TestOptions.() -> Unit = {
            unitTests.isIncludeAndroidResources = true
        }

        when {
            project.plugins.hasPlugin("com.android.application") ->
                project.androidApp { testOptions(configureTestResources) }

            project.plugins.hasPlugin("com.android.library") ->
                project.androidLibrary {
                    testOptions {
                        configureTestResources()
                        unitTests.all(Test::defaultTestSetup)
                    }
                }

            project.plugins.hasPlugin("com.android.test") ->
                project.extensions.configure(TestExtension::class.java) {
                    it.testOptions(
                        configureTestResources,
                    )
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

private fun Project.androidSetup() {
    val isLibrary = plugins.hasPlugin("com.android.library")
    val desugarLibrary = getDependencyOrNull("android-desugarJdkLibs")

    configureCommonAndroid()

    android {
        (defaultConfig as? ApplicationDefaultConfig)?.let {
            it.targetSdk = getVersion("android-target").toInt()
        }
    }

    val configureCompileOptions: CompileOptions.() -> Unit = {
        isCoreLibraryDesugaringEnabled = desugarLibrary != null
        sourceCompatibility = javaTargetVersion.get()
        targetCompatibility = javaTargetVersion.get()
    }

    if (isLibrary) {
        androidLibrary {
            buildFeatures {
                viewBinding = false
                resValues = false
                shaders = false
            }
            compileOptions(configureCompileOptions)
        }
    } else {
        androidApp {
            compileOptions(configureCompileOptions)
        }
    }

    dependencies.addIfNotNull("coreLibraryDesugaring", desugarLibrary)
}
