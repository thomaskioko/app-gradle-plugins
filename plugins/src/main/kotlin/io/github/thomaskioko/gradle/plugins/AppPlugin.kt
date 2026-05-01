package io.github.thomaskioko.gradle.plugins

import io.github.thomaskioko.gradle.plugins.extensions.AppExtension
import io.github.thomaskioko.gradle.plugins.properties.PropertyKeys
import io.github.thomaskioko.gradle.plugins.properties.scaffoldProperties
import io.github.thomaskioko.gradle.plugins.utils.androidApp
import io.github.thomaskioko.gradle.plugins.utils.androidComponents
import io.github.thomaskioko.gradle.plugins.utils.baseExtension
import io.github.thomaskioko.gradle.plugins.utils.disableAndroidApplicationTasks
import io.github.thomaskioko.gradle.plugins.utils.isDebugOnlyBuild
import io.github.thomaskioko.gradle.plugins.utils.parseKeyValueFile
import io.github.thomaskioko.gradle.tasks.release.BumpVersionTask
import io.github.thomaskioko.gradle.tasks.release.ReleaseTask
import org.gradle.api.Plugin
import org.gradle.api.Project

public abstract class AppPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("com.android.application")
        target.plugins.apply(AndroidPlugin::class.java)

        target.baseExtension.extensions.create("app", AppExtension::class.java)

        val properties = target.scaffoldProperties()

        val versionFile = target.rootProject.file("version.txt")
        target.tasks.register("bumpVersion", BumpVersionTask::class.java) {
            it.versionFile.set(versionFile)
            it.bumpType.convention(properties.releaseType)
        }

        val changelogFile = target.rootProject.file("CHANGELOG.md")
        val cliffConfig = target.rootProject.file("cliff.toml")
        target.tasks.register("release", ReleaseTask::class.java) {
            it.versionFile.set(versionFile)
            it.changelogFile.set(changelogFile)
            it.cliffConfigFile.set(cliffConfig)
            it.projectDir.set(target.rootProject.layout.projectDirectory)
            it.bumpType.convention(properties.releaseType)
            it.beta.convention(properties.releaseBeta)
            it.interactive.convention(properties.releaseInteractive)
            it.dryRun.convention(properties.releaseDryRun)
        }

        target.androidApp {
            buildFeatures {
                buildConfig = true
            }

            val versionProps = target.parseKeyValueFile("version.txt")
            val resolvedVersionName = requireNotNull(versionProps["VERSION_NUMBER"]) {
                "VERSION_NUMBER not found in version.txt. Ensure version.txt exists at the project root."
            }
            val resolvedBuildNumber = requireNotNull(versionProps["BUILD_NUMBER"]?.toIntOrNull()) {
                "BUILD_NUMBER not found or not a valid integer in version.txt."
            }

            val versionSuffix = properties.appVersionSuffix.get()

            defaultConfig {
                versionCode = resolvedBuildNumber
                versionName = resolvedVersionName + versionSuffix
                manifestPlaceholders["appAuthRedirectScheme"] = "app"
            }

            signingConfigs {
                named("debug") {
                    val debugKeystore = target.rootProject.file("gradle/debug.keystore")
                    if (debugKeystore.exists()) {
                        it.storeFile = debugKeystore
                    }
                    // enable v4 signing for incremental installs on Android 12
                    it.enableV3Signing = true
                    it.enableV4Signing = true
                }

                val keyStoreFile = properties.releaseStoreFile
                if (keyStoreFile.isPresent) {
                    val missingProps = buildList {
                        if (!properties.releaseStorePassword.isPresent) add(PropertyKeys.RELEASE_STORE_PASSWORD)
                        if (!properties.releaseKeyAlias.isPresent) add(PropertyKeys.RELEASE_KEY_ALIAS)
                        if (!properties.releaseKeyPassword.isPresent) add(PropertyKeys.RELEASE_KEY_PASSWORD)
                    }

                    require(missingProps.isEmpty()) {
                        "Release signing requires these properties in gradle.properties or local.properties: ${missingProps.joinToString()}"
                    }

                    register("release") {
                        it.storeFile = target.rootProject.file(keyStoreFile.get())
                        it.storePassword = properties.releaseStorePassword.get()
                        it.keyAlias = properties.releaseKeyAlias.get()
                        it.keyPassword = properties.releaseKeyPassword.get()
                        it.enableV3Signing = true
                        it.enableV4Signing = true
                    }
                }
            }

            buildTypes {
                named("debug") {
                    it.applicationIdSuffix = ".debug"
                    it.signingConfig = signingConfigs.getByName("debug")
                }

                named("release") {
                    it.signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
                    it.ndk.debugSymbolLevel = "FULL"
                    it.isMinifyEnabled = true // Enables code-related app optimization.
                    it.isShrinkResources = true // Enables resource shrinking.
                }
            }

            lint {
                baseline = target.file("lint-baseline.xml")
            }
        }

        target.androidComponents {
            // Disable non-debug variants when debugOnly is enabled
            if (target.isDebugOnlyBuild()) {
                beforeVariants { variant ->
                    variant.enable = variant.buildType == "debug"
                }
            }

            onVariants(selector().withBuildType("release")) {
                it.packaging.resources.excludes.addAll(
                    // produced by Kotlin for each module/library that uses it, they are not needed inside an app
                    "kotlin/*.kotlin_builtins",
                    "kotlin/**/*.kotlin_builtins",
                    "META-INF/*.kotlin_module",
                    // these are part of AndroidX and only contain version information
                    "META-INF/*.version",
                    // these contain build meta data for various Google Play and Firebase libraries
                    "core.properties",
                    "billing.properties",
                    "build-data.properties",
                    "play-services-*.properties",
                    "firebase-*.properties",
                    "transport-*.properties",
                    // metadata with which Kotlin version the app was built
                    "kotlin-tooling-metadata.json",
                    // various license files or other random files that were packaged in some libraries
                    "asm-license.txt",
                    "LICENSE",
                    "LICENSE_OFL",
                    "LICENSE_UNICODE",
                    "LICENSE.txt",
                    "NOTICE",
                    "README.txt",
                    "META-INF/LICENSE.txt",
                    "META-INF/LICENSE-FIREBASE.txt",
                    "META-INF/LICENSE",
                    "META-INF/NOTICE.txt",
                    "META-INF/NOTICE",
                    "META-INF/DEPENDENCIES.txt",
                )
            }
        }

        target.disableAndroidApplicationTasks()
    }
}
