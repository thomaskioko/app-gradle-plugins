import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-gradle-plugin")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.publish)
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
    implementation(libs.compose.compiler.gradle.plugin)
    implementation(libs.dependency.analysis.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin.api)
    implementation(libs.ksp.gradle)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.moko.resources)
    implementation(libs.moko.resources.generator)

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

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    if (providers.gradleProperty("maven.central.publish").orNull == "true") {
        signAllPublications()
    }

    pom {
        name.set(providers.gradleProperty("POM_NAME").get())
        description.set(providers.gradleProperty("POM_DESCRIPTION").get())
        inceptionYear.set(providers.gradleProperty("INCEPTION_YEAR").get())
        url.set(providers.gradleProperty("POM_URL").get())

        licenses {
            license {
                name.set(providers.gradleProperty("POM_LICENSE_NAME").get())
                url.set(providers.gradleProperty("POM_LICENSE_URL").get())
                distribution.set(providers.gradleProperty("POM_LICENSE_DIST").get())
            }
        }

        developers {
            developer {
                id.set(providers.gradleProperty("POM_DEVELOPER_ID").get())
                name.set(providers.gradleProperty("POM_DEVELOPER_NAME").get())
                url.set(providers.gradleProperty("POM_DEVELOPER_URL").get())
            }
        }

        scm {
            url.set(providers.gradleProperty("POM_SCM_URL").get())
            connection.set(providers.gradleProperty("POM_SCM_CONNECTION").get())
            developerConnection.set(providers.gradleProperty("POM_SCM_DEV_CONNECTION").get())
        }
    }
}
