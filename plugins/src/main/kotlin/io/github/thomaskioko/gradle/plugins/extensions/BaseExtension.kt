package io.github.thomaskioko.gradle.plugins.extensions

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.Lint
import io.github.thomaskioko.gradle.plugins.setup.setupCodegen
import io.github.thomaskioko.gradle.plugins.setup.setupKotlinInject
import io.github.thomaskioko.gradle.plugins.setup.setupMetro
import io.github.thomaskioko.gradle.plugins.setup.setupSerialization
import io.github.thomaskioko.gradle.plugins.utils.compilerOptions
import io.github.thomaskioko.gradle.plugins.utils.getPackageNameProvider
import io.github.thomaskioko.gradle.plugins.utils.jvmCompilerOptions
import io.github.thomaskioko.gradle.plugins.utils.jvmTarget
import io.github.thomaskioko.gradle.plugins.utils.kotlin
import io.github.thomaskioko.gradle.plugins.utils.kotlinMultiplatform
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkConfig
import java.net.URI

@ScaffoldDsl
public abstract class BaseExtension(private val project: Project) : ExtensionAware {
    public fun optIn(vararg classes: String) {
        project.kotlin {
            compilerOptions {
                optIn.addAll(*classes)
            }
        }
    }

    public fun useMetro() {
        project.setupMetro()
    }

    public fun useSerialization() {
        project.setupSerialization()
    }

    public fun useKotlinInject() {
        project.setupKotlinInject()
    }

    public fun useCodegen() {
        project.setupCodegen()
    }

    public fun android(configure: AndroidExtension.() -> Unit) {
        val androidExtension = extensions.findByType(AndroidExtension::class.java)
            ?: throw IllegalStateException("Android extension not found. Did you call addAndroidTarget()?")
        androidExtension.configure()
    }

    @JvmOverloads
    public fun addAndroidTarget(
        enableAndroidResources: Boolean = false,
        withDeviceTestBuilder: Boolean = false,
        withJava: Boolean = false,
        configure: AndroidExtension.() -> Unit = { },
        lintConfiguration: Lint.() -> Unit = { },
    ) {
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

                lint.lintConfiguration()
            }
        }

        extensions.create("android", AndroidExtension::class.java)
            .configure()
    }

    public fun addJvmTarget() {
        project.kotlinMultiplatform {
            jvm()
        }
    }

    @JvmOverloads
    public fun addIosTargets(includeX64: Boolean = false) {
        project.kotlinMultiplatform {
            iosArm64()
            iosSimulatorArm64()
            if (includeX64) {
                iosX64()
            }
        }
    }

    @JvmOverloads
    public fun configureNativeTargets(
        bundleId: String? = null,
        configure: KotlinNativeTarget.() -> Unit = {},
    ) {
        project.kotlinMultiplatform {
            targets.withType(KotlinNativeTarget::class.java).configureEach { target ->
                target.binaries.configureEach { framework ->
                    framework.linkerOpts("-lsqlite3")
                    framework.binaryOption(
                        "bundleId",
                        bundleId ?: project.getPackageNameProvider().get(),
                    )
                }

                target.compilations.configureEach { compilation ->
                    compilation.compileTaskProvider.configure { compileTask ->
                        compileTask.compilerOptions.freeCompilerArgs.addAll(
                            "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                            "-opt-in=kotlinx.cinterop.BetaInteropApi",
                            "-Xallocator=custom",
                            "-Xadd-light-debug=enable",
                            "-Xexpect-actual-classes",
                        )
                    }
                }

                target.configure()
            }
        }
    }

    @JvmOverloads
    public fun addIosTargetsWithXcFramework(
        frameworkName: String,
        includeX64: Boolean = false,
        configure: KotlinNativeTarget.(Framework) -> Unit = { },
    ) {
        addIosTargets(includeX64)

        val xcFramework = XCFrameworkConfig(project, frameworkName)

        project.kotlinMultiplatform {
            targets.withType(KotlinNativeTarget::class.java).configureEach {
                it.binaries.framework {
                    baseName = frameworkName
                    isStatic = true

                    disableNativeCacheForCurrentKotlin(
                        reason = "Kotlin/Native cache bug causes double runtime injection when linking multiple frameworks. See KT-42254.",
                        issueUrl = URI("https://youtrack.jetbrains.com/issue/KT-42254"),
                    )

                    xcFramework.add(this)
                    it.configure(this)
                }
            }
        }
    }
}
