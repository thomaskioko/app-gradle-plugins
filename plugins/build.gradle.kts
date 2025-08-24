import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-gradle-plugin")
    alias(libs.plugins.kotlin.jvm)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }

    withType<KotlinCompile>().configureEach {
        compilerOptions {
            allWarningsAsErrors.set(false)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-Xjvm-default=all"
            )
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get()))
    }
}

kotlin {
    explicitApi()
}

dependencies {
    api(libs.kotlin.gradle.plugin)
    implementation(libs.android.gradle.plugin)
    implementation(libs.moko.resources.generator)
    implementation(libs.compose.compiler.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin.api)
    implementation(libs.dependency.analysis.gradle.plugin)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.moko.resources)

    compileOnly(libs.baselineprofile.gradlePlugin)
    compileOnly(libs.skie.gradle.plugin)
    compileOnly(libs.spotless.plugin)
    implementation(libs.gradle.doctor.gradle.plugin)
    runtimeOnly(libs.compose.compiler.gradle.plugin)
}

gradlePlugin {
    plugins.create("appPlugin") {
        id = "com.thomaskioko.gradle.app"
        implementationClass = "com.thomaskioko.gradle.plugin.AppPlugin"
    }

    plugins.create("androidPlugin") {
        id = "com.thomaskioko.gradle.android"
        implementationClass = "com.thomaskioko.gradle.plugin.AndroidPlugin"
    }

    plugins.create("androidMultiplatformPlugin") {
        id = "com.thomaskioko.gradle.android.multiplatform"
        implementationClass = "com.thomaskioko.gradle.plugin.AndroidMultiplatformPlugin"
    }

    plugins.create("jvmPlugin") {
        id = "com.thomaskioko.gradle.jvm"
        implementationClass = "com.thomaskioko.gradle.plugin.JvmPlugin"
    }

    plugins.create("baselineProfilePlugin") {
        id = "com.thomaskioko.gradle.baseline.profile"
        implementationClass = "com.thomaskioko.gradle.plugin.BaselineProfilePlugin"
    }

    plugins.create("commonMultiplatformPlugin") {
        id = "com.thomaskioko.gradle.multiplatform"
        implementationClass = "com.thomaskioko.gradle.plugin.KotlinMultiplatformPlugin"
    }

    plugins.create("basePlugin") {
        id = "com.thomaskioko.gradle.base"
        implementationClass = "com.thomaskioko.gradle.plugin.BasePlugin"
    }

    plugins.create("rootPlugin") {
        id = "com.thomaskioko.gradle.root"
        implementationClass = "com.thomaskioko.gradle.plugin.RootPlugin"
    }

    plugins.create("spotlessPlugin") {
        id = "com.thomaskioko.gradle.spotless"
        implementationClass = "com.thomaskioko.gradle.plugin.checks.SpotlessPlugin"
    }

    plugins.create("resourceGeneratorPlugin") {
        id = "com.thomaskioko.resource.generator"
        implementationClass = "com.thomaskioko.gradle.plugin.ResourceGeneratorPlugin"
    }
}

