package io.github.thomaskioko.gradle.plugins.extensions

import co.touchlab.skie.configuration.DefaultArgumentInterop
import co.touchlab.skie.configuration.EnumInterop
import co.touchlab.skie.configuration.FlowInterop
import co.touchlab.skie.configuration.SealedInterop
import co.touchlab.skie.configuration.SuppressSkieWarning
import co.touchlab.skie.configuration.SuspendInterop
import co.touchlab.skie.plugin.configuration.SkieExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.LibraryExtension
import io.github.thomaskioko.gradle.plugins.AndroidMultiplatformPlugin
import io.github.thomaskioko.gradle.plugins.AndroidPlugin
import io.github.thomaskioko.gradle.plugins.utils.addBundleImplementationDependency
import io.github.thomaskioko.gradle.plugins.utils.addImplementationDependency
import io.github.thomaskioko.gradle.plugins.utils.addKspDependencyForAllTargets
import io.github.thomaskioko.gradle.plugins.utils.compilerOptions
import io.github.thomaskioko.gradle.plugins.utils.configureProcessing
import io.github.thomaskioko.gradle.plugins.utils.getBundleDependencies
import io.github.thomaskioko.gradle.plugins.utils.getDependency
import io.github.thomaskioko.gradle.plugins.utils.jvmCompilerOptions
import io.github.thomaskioko.gradle.plugins.utils.jvmTarget
import io.github.thomaskioko.gradle.plugins.utils.kotlin
import io.github.thomaskioko.gradle.plugins.utils.kotlinMultiplatform
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkConfig

public abstract class BaseExtension(private val project: Project) : ExtensionAware {
    public fun optIn(vararg classes: String) {
        project.kotlin {
            compilerOptions {
                optIn.addAll(*classes)
            }
        }
    }

    public fun useSkie(
        defaultArgumentInterop: Boolean = false,
        suspendInterop: Boolean = true,
        flowInterop: Boolean = true,
        enumInterop: Boolean = true,
        sealedInterop: Boolean = true,
        suppressSkieWarning: Boolean = true,
    ) {
        project.plugins.apply("co.touchlab.skie")

        project.extensions.configure(SkieExtension::class.java) { extension ->
            extension.analytics {
                it.disableUpload.set(true)
            }

            extension.features {
                it.group {
                    DefaultArgumentInterop.Enabled(defaultArgumentInterop)
                    SuspendInterop.Enabled(suspendInterop)
                    FlowInterop.Enabled(flowInterop)
                    EnumInterop.Enabled(enumInterop)
                    SealedInterop.Enabled(sealedInterop)
                    SuppressSkieWarning.NameCollision(suppressSkieWarning)
                }
            }
        }
    }

    public fun useMetro() {
        project.plugins.apply("dev.zacsweers.metro")

        project.addImplementationDependency(project.getDependency("metro-runtime"))
    }

    public fun useSerialization() {
        project.plugins.apply("org.jetbrains.kotlin.plugin.serialization")

        project.addImplementationDependency(project.getDependency("kotlin-serialization-core"))
    }

    public fun useKotlinInject() {
        project.configureProcessing()

        project.addBundleImplementationDependency(project.getBundleDependencies("kotlinInject"))
        project.addKspDependencyForAllTargets(project.getDependency("kotlinInject-compiler"))
        project.addKspDependencyForAllTargets(project.getDependency("kotlinInject-anvil-compiler"))
    }

    public fun android(configure: AndroidExtension.() -> Unit) {
        val androidExtension = extensions.findByType(AndroidExtension::class.java)
            ?: throw IllegalStateException("Android extension not found. Did you call addAndroidTarget()?")
        androidExtension.configure()
    }

    @Deprecated("Use addAndroidMultiplatformTarget instead")
    @JvmOverloads
    public fun addAndroidTarget(
        configure: AndroidExtension.() -> Unit = { },
        libraryConfiguration: LibraryExtension.() -> Unit = { },
    ) {
        project.plugins.apply(AndroidPlugin::class.java)

        project.kotlinMultiplatform {
            androidTarget()
        }

        project.extensions.configure(LibraryExtension::class.java) { extension ->
            extension.libraryConfiguration()
        }

        val androidExtension = extensions.findByType(AndroidExtension::class.java)
            ?: throw IllegalStateException("Android extension not found. AndroidPlugin should have created it.")
        androidExtension.configure()
    }

    @JvmOverloads
    public fun addAndroidMultiplatformTarget(
        withDeviceTestBuilder: Boolean = false,
        enableAndroidResources: Boolean = false,
        withJava: Boolean = false,
        action: KotlinMultiplatformAndroidLibraryTarget.() -> Unit = { },
    ) {
        project.plugins.apply(AndroidMultiplatformPlugin::class.java)

        project.kotlinMultiplatform {
            extensions.findByType(KotlinMultiplatformAndroidLibraryTarget::class.java)?.apply {
                if (enableAndroidResources) {
                    androidResources.enable = true
                }

                if (withDeviceTestBuilder) {
                    withDeviceTestBuilder {
                        sourceSetTreeName = "test"
                    }.configure {
                        instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    }
                }

                if (withJava) {
                    withJava()
                    jvmCompilerOptions {
                        jvmTarget.set(project.jvmTarget)
                    }
                }

                action()
            }
        }
    }

    @JvmOverloads
    public fun addIosTargetsWithXcFramework(
        frameworkName: String,
        configure: KotlinNativeTarget.(Framework) -> Unit = { },
    ) {
        val xcFramework = XCFrameworkConfig(project, frameworkName)

        project.kotlinMultiplatform {
            targets.withType(KotlinNativeTarget::class.java).configureEach {
                it.binaries.framework {
                    baseName = frameworkName

                    xcFramework.add(this)
                    it.configure(this)
                }
            }
        }
    }
}
