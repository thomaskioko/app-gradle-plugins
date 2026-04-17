import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-gradle-plugin")
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.publish)
}

group = property("GROUP").toString()
version = property("VERSION_NAME").toString()

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
    api(libs.kotlin.gradle.plugin.api)
    api(libs.android.gradle.plugin.api)

    runtimeOnly(libs.android.gradle.plugin)

    implementation(libs.compose.compiler.gradle.plugin)
    implementation(libs.dependency.analysis.gradle.plugin)
    implementation(libs.ksp.gradle)
    implementation(libs.kotlinpoet)
    implementation(libs.gradle.doctor.gradle.plugin)

    compileOnly(libs.baselineprofile.gradlePlugin)
    compileOnly(libs.metro.gradle.plugin)
    compileOnly(libs.spotless.gradle.plugin)
    implementation(libs.mordant.core)

    testImplementation(libs.junit)
    testImplementation(gradleTestKit())
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

    plugins.create("buildConfigGeneratorPlugin") {
        id = "io.github.thomaskioko.gradle.plugins.buildconfig"
        implementationClass = "io.github.thomaskioko.gradle.plugins.BuildConfigGeneratorPlugin"
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    if (findProperty("maven.central.publish")?.toString() == "true") {
        signAllPublications()
    }

    pom {
        name.set(property("POM_NAME").toString())
        description.set(property("POM_DESCRIPTION").toString())
        inceptionYear.set(property("INCEPTION_YEAR").toString())
        url.set(property("POM_URL").toString())

        licenses {
            license {
                name.set(property("POM_LICENSE_NAME").toString())
                url.set(property("POM_LICENSE_URL").toString())
                distribution.set(property("POM_LICENSE_DIST").toString())
            }
        }

        developers {
            developer {
                id.set(property("POM_DEVELOPER_ID").toString())
                name.set(property("POM_DEVELOPER_NAME").toString())
                url.set(property("POM_DEVELOPER_URL").toString())
            }
        }

        scm {
            url.set(property("POM_SCM_URL").toString())
            connection.set(property("POM_SCM_CONNECTION").toString())
            developerConnection.set(property("POM_SCM_DEV_CONNECTION").toString())
        }
    }
}
