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
        id = "io.github.thomaskioko.gradle.plugins.app"
        implementationClass = "io.github.thomaskioko.gradle.plugins.AppPlugin"
    }

    plugins.create("androidPlugin") {
        id = "io.github.thomaskioko.gradle.plugins.android"
        implementationClass = "io.github.thomaskioko.gradle.plugins.AndroidPlugin"
    }

    plugins.create("androidMultiplatformPlugin") {
        id = "io.github.thomaskioko.gradle.plugins.android.multiplatform"
        implementationClass = "io.github.thomaskioko.gradle.plugins.AndroidMultiplatformPlugin"
    }

    plugins.create("jvmPlugin") {
        id = "io.github.thomaskioko.gradle.plugins.jvm"
        implementationClass = "io.github.thomaskioko.gradle.plugins.JvmPlugin"
    }

    plugins.create("baselineProfilePlugin") {
        id = "io.github.thomaskioko.gradle.plugins.baseline.profile"
        implementationClass = "io.github.thomaskioko.gradle.plugins.BaselineProfilePlugin"
    }

    plugins.create("commonMultiplatformPlugin") {
        id = "io.github.thomaskioko.gradle.plugins.multiplatform"
        implementationClass = "io.github.thomaskioko.gradle.plugins.KotlinMultiplatformPlugin"
    }

    plugins.create("basePlugin") {
        id = "io.github.thomaskioko.gradle.plugins.base"
        implementationClass = "io.github.thomaskioko.gradle.plugins.BasePlugin"
    }

    plugins.create("rootPlugin") {
        id = "io.github.thomaskioko.gradle.plugins.root"
        implementationClass = "io.github.thomaskioko.gradle.plugins.RootPlugin"
    }

    plugins.create("spotlessPlugin") {
        id = "io.github.thomaskioko.gradle.plugins.spotless"
        implementationClass = "io.github.thomaskioko.gradle.plugins.checks.SpotlessPlugin"
    }

    plugins.create("resourceGeneratorPlugin") {
        id = "io.github.thomaskioko.gradle.plugins.resource.generator"
        implementationClass = "io.github.thomaskioko.gradle.plugins.ResourceGeneratorPlugin"
    }
}

