package io.github.thomaskioko.gradle.plugins.extensions

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.Lint
import com.autonomousapps.DependencyAnalysisExtension
import io.github.thomaskioko.gradle.plugins.setup.setupCodegen
import io.github.thomaskioko.gradle.plugins.setup.setupFeatureFlagCodegen
import io.github.thomaskioko.gradle.plugins.setup.setupKotlinInject
import io.github.thomaskioko.gradle.plugins.setup.setupDependencyGuard
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

/**
 * Top-level receiver for the `scaffold {}` DSL exposed by every plugin in this suite.
 *
 * Each method on this class either configures a Kotlin compiler feature (such as opt-in flags),
 * enables an opinionated tool (Metro, Serialization, kotlin-inject, the navigation codegen), or
 * adds a Kotlin Multiplatform target. Sub-DSLs for Android and JVM apps are reachable through
 * [android] once [addAndroidTarget] has registered the Android sub-extension.
 *
 * ```kotlin
 * scaffold {
 *   useMetro()
 *   addJvmTarget()
 *   addAndroidTarget {
 *     useCompose()
 *     minSdkVersion(24)
 *   }
 *   addIosTargets()
 * }
 * ```
 *
 * @property project The Gradle [Project] this extension is attached to. Used internally to apply
 *   plugins, register dependencies, and configure the Kotlin Multiplatform extension.
 */
@ScaffoldDsl
public abstract class BaseExtension(private val project: Project) : ExtensionAware {
    /**
     * Excludes the named project dependencies from this module's "unused dependencies" check.
     *
     * The call routes to the root project's `DependencyAnalysisExtension` and scopes the exclusion
     * to the module the scaffold is attached to, replacing a hand-written project-scoped
     * `configure<DependencyAnalysisSubExtension> { issues { onUnusedDependencies { exclude(...) } } }`
     * block. Appropriate when a module declares a dependency as `api(...)` for downstream consumers
     * (for example integration-test fixtures) that its own sources never reference, so the analysis
     * reports the dependency as unused.
     *
     * ```kotlin
     * scaffold {
     *   ignoreUnusedDependencies(":data:request-manager:testing")
     * }
     * ```
     *
     * @param dependencyPaths Gradle project dependency paths (for example
     *   `":data:request-manager:testing"`) to exclude from this module's unused-dependency check.
     */
    public fun ignoreUnusedDependencies(vararg dependencyPaths: String) {
        project.rootProject.extensions.configure(DependencyAnalysisExtension::class.java) { analysis ->
            analysis.issues { issues ->
                issues.project(project.path) { perProject ->
                    perProject.onUnusedDependencies { it.exclude(*dependencyPaths) }
                }
            }
        }
    }

    /**
     * Silences every DAGP issue category (`onAny`) for the named Gradle project paths. Use
     * sparingly.
     *
     * The call routes to the root project's `DependencyAnalysisExtension`, so the named module's
     * analysis output is suppressed regardless of which scaffold the call lives in. Appropriate
     * for a Kotlin/Native-only framework module whose dependencies are entirely consumed by the
     * iOS app and never by another JVM or Android consumer, where DAGP's model genuinely does
     * not apply. Replaces the previous hardcoded `AnalysisExclusions.ignoredModules` list with
     * an explicit per-module opt-in.
     *
     * Called with no arguments, it silences the project the scaffold is attached to:
     *
     * ```kotlin
     * // ios-framework/build.gradle.kts
     * scaffold {
     *   ignoreAll()
     * }
     * ```
     *
     * @param projectPaths Gradle project paths (for example `":ios-framework"`) whose DAGP
     *   analysis output should be silenced. Defaults to the current project when empty.
     */
    public fun ignoreAll(vararg projectPaths: String) {
        val paths = if (projectPaths.isEmpty()) arrayOf(project.path) else projectPaths
        project.rootProject.extensions.configure(DependencyAnalysisExtension::class.java) { analysis ->
            analysis.issues { issues ->
                paths.forEach { projectPath ->
                    issues.project(projectPath) { perProject ->
                        perProject.onAny { it.severity("ignore") }
                    }
                }
            }
        }
    }

    /**
     * Adds compiler opt-in entries to every Kotlin compilation in the project.
     *
     * Use this to opt in to experimental APIs without sprinkling `@OptIn` annotations across the
     * source set. Each entry is a fully qualified annotation class name.
     *
     * ```kotlin
     * scaffold {
     *   optIn(
     *     "kotlinx.coroutines.ExperimentalCoroutinesApi",
     *     "kotlinx.serialization.ExperimentalSerializationApi",
     *   )
     * }
     * ```
     *
     * @param classes Fully qualified annotation class names to add to the Kotlin compiler's
     *   `optIn` list.
     */
    public fun optIn(vararg classes: String) {
        project.kotlin {
            compilerOptions {
                optIn.addAll(*classes)
            }
        }
    }

    /**
     * Applies the Metro Gradle plugin and turns on contribution providers.
     *
     * Call this in a module that participates in a Metro dependency graph. The plugin registers
     * the Metro compiler plugin on every Kotlin compilation in the project and is required for
     * `@DependencyGraph`, `@Inject`, `@Provides`, and contribution annotations to compile.
     *
     * ```kotlin
     * scaffold {
     *   useMetro()
     * }
     * ```
     */
    public fun useMetro() {
        project.setupMetro()
    }

    /**
     * Applies the dependency-guard plugin and guards the given resolvable configurations. Use this on
     * modules whose shipped dependency set should be baselined, for example a Kotlin Multiplatform
     * framework umbrella. Android application modules guard `releaseRuntimeClasspath` automatically.
     *
     * ```kotlin
     * scaffold {
     *   useDependencyGuard("iosArm64CompileKlibraries")
     * }
     * ```
     */
    public fun useDependencyGuard(vararg configurations: String) {
        project.setupDependencyGuard(*configurations)
    }

    /**
     * Applies the kotlinx.serialization Gradle plugin and adds the runtime dependency.
     *
     * Use this in any module that defines `@Serializable` classes. The runtime artifact is
     * resolved from the consumer's version catalog under the `kotlinx-serialization-json` alias.
     *
     * ```kotlin
     * scaffold {
     *   useSerialization()
     * }
     * ```
     */
    public fun useSerialization() {
        project.setupSerialization()
    }

    /**
     * Applies the kotlin-inject and kotlin-inject-anvil compiler plugins.
     *
     * Use this in modules that rely on kotlin-inject for dependency injection. Pairs with the
     * runtime and KSP dependencies that the setup helper wires in. Mutually exclusive with
     * [useMetro] in the same module.
     *
     * ```kotlin
     * scaffold {
     *   useKotlinInject()
     * }
     * ```
     */
    public fun useKotlinInject() {
        project.setupKotlinInject()
    }

    /**
     * Applies KSP and adds the navigation codegen processor and annotations.
     *
     * The codegen reads `@NavDestination`, `@ScreenUi`, and `@SheetUi` annotations and produces
     * Metro `@GraphExtension` graphs plus binding files. See
     * [codegen/docs/get-started.md](../../../../../../../../codegen/docs/get-started.md) for
     * the full feature list.
     *
     * ```kotlin
     * scaffold {
     *   useMetro()
     *   useCodegen()
     * }
     * ```
     */
    public fun useCodegen() {
        project.setupCodegen()
    }

    /**
     * Applies KSP and adds the feature flag codegen processor and annotations.
     *
     * The codegen reads `@FeatureFlag`-decorated qualifier annotations and emits one
     * `<QualifierBaseName>Binding.kt` per qualifier into the qualifier's package. Each generated
     * file contains a `@ContributesTo(AppScope::class)` interface with two `@Provides` methods that
     * wire the flag through the consumer's `FeatureFlagFactory` and into the
     * `Set<FeatureFlag<Boolean>>` multibinding. See `codegen/docs/featureflag.md` for the full
     * contract.
     *
     * Independent from [useCodegen]; modules that consume both navigation and feature flag codegen
     * call both functions.
     *
     * ```kotlin
     * scaffold {
     *   useMetro()
     *   useFeatureFlagCodegen()
     * }
     * ```
     */
    public fun useFeatureFlagCodegen() {
        project.setupFeatureFlagCodegen()
    }

    /**
     * Configures the Android sub-DSL after [addAndroidTarget] has registered it.
     *
     * Calling this before [addAndroidTarget] throws an `IllegalStateException`. The receiver of
     * the lambda is an [AndroidExtension] that exposes Android-specific options such as
     * [AndroidExtension.useCompose], [AndroidExtension.enableAndroidTests], and
     * [AndroidExtension.manifestPlaceholders].
     *
     * ```kotlin
     * scaffold {
     *   addAndroidTarget()
     *   android {
     *     useCompose()
     *     enableAndroidResources()
     *   }
     * }
     * ```
     *
     * @param configure Configuration block applied to the registered [AndroidExtension].
     */
    public fun android(configure: AndroidExtension.() -> Unit) {
        val androidExtension = extensions.findByType(AndroidExtension::class.java)
            ?: throw IllegalStateException("Android extension not found. Did you call addAndroidTarget()?")
        androidExtension.configure()
    }

    /**
     * Adds an Android library target to the Kotlin Multiplatform extension and registers the
     * Android sub-DSL.
     *
     * Configures the Kotlin Multiplatform Android library target with project defaults and
     * registers an `android` sub-extension on this receiver. After this call, you can configure
     * Android-specific options inside the [configure] block or through a separate [android] call.
     *
     * ```kotlin
     * scaffold {
     *   addAndroidTarget(
     *     enableAndroidResources = true,
     *     withDeviceTestBuilder = true,
     *   ) {
     *     useCompose()
     *     minSdkVersion(24)
     *   }
     * }
     * ```
     *
     * @param enableAndroidResources Enables Android resource processing on the target. Defaults
     *   to `false` because most shared modules ship code only.
     * @param withDeviceTestBuilder Registers an Android device test builder wired to
     *   `androidx.test.runner.AndroidJUnitRunner`. Set to `true` for modules that need
     *   instrumentation tests.
     * @param withJava Enables Java sources on the Android target and aligns the JVM compiler
     *   options to the project's [jvmTarget]. Defaults to `false`.
     * @param configure Configuration block applied to the registered [AndroidExtension].
     * @param lintConfiguration Configuration block applied to the target's [Lint] options.
     */
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

    /**
     * Adds the JVM target to the Kotlin Multiplatform extension.
     *
     * Use this when a shared module produces a JVM artifact, for example a desktop app, a server,
     * or a CLI tool. The target inherits the JVM compile options from the project's
     * [jvmTarget].
     *
     * ```kotlin
     * scaffold {
     *   addJvmTarget()
     * }
     * ```
     */
    public fun addJvmTarget() {
        project.kotlinMultiplatform {
            jvm()
        }
    }

    /**
     * Adds the iOS targets used by the project: `iosArm64` and `iosSimulatorArm64`.
     *
     * The `iosX64` target is omitted by default because Apple Silicon hosts run the simulator on
     * `iosSimulatorArm64`. Set [includeX64] to `true` only when an Intel Mac or CI runner needs
     * to compile or test against the x86_64 simulator.
     *
     * ```kotlin
     * scaffold {
     *   addIosTargets()
     * }
     * ```
     *
     * @param includeX64 Adds the `iosX64` target alongside the arm64 targets. Defaults to
     *   `false`.
     */
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

    /**
     * Applies common compiler and linker options to every Kotlin/Native target in the project.
     *
     * Sets the framework `bundleId` binary option, links `sqlite3`, opts in to the Foreign and
     * Beta interop APIs, picks the custom Kotlin/Native allocator, enables light debug info, and
     * allows expect/actual classes. Run this after [addIosTargets] or
     * [addIosTargetsWithXcFramework].
     *
     * ```kotlin
     * scaffold {
     *   addIosTargets()
     *   configureNativeTargets(bundleId = "com.example.shared")
     * }
     * ```
     *
     * @param bundleId Bundle identifier written into every framework binary. When `null`, the
     *   project's package name from the version catalog is used.
     * @param configure Per-target configuration block applied after the common options. Receives
     *   each [KotlinNativeTarget] in turn.
     */
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

    /**
     * Adds the iOS targets and bundles them into a single static XCFramework.
     *
     * Calls [addIosTargets] first, then registers each iOS target's framework with an
     * [XCFrameworkConfig] keyed by [frameworkName]. The framework is static (`isStatic = true`),
     * which is the configuration the Tv Maniac iOS app expects. Also applies the Kotlin/Native
     * cache workaround from
     * [KT-42254](https://youtrack.jetbrains.com/issue/KT-42254) so multiple frameworks can be
     * linked without double runtime injection.
     *
     * ```kotlin
     * scaffold {
     *   addIosTargetsWithXcFramework("Shared") {
     *     export(project(":api"))
     *   }
     *   configureNativeTargets()
     * }
     * ```
     *
     * @param frameworkName Base name for the produced framework and XCFramework directory.
     * @param includeX64 Adds the `iosX64` target alongside the arm64 targets. Defaults to
     *   `false`.
     * @param configure Per-target configuration block. Receives the [KotlinNativeTarget] and the
     *   target's [Framework] so additional `export(...)` or compiler options can be added.
     */
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
