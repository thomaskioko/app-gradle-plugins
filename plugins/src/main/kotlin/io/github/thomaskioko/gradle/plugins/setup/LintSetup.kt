package io.github.thomaskioko.gradle.plugins.setup

import com.android.build.api.dsl.Lint
import io.github.thomaskioko.gradle.plugins.utils.addIfNotNull
import io.github.thomaskioko.gradle.plugins.utils.android
import io.github.thomaskioko.gradle.plugins.utils.getBundleDependenciesOrNull
import io.github.thomaskioko.gradle.plugins.utils.getVersion
import io.github.thomaskioko.gradle.plugins.utils.pathBasedAndroidNamespace
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

internal fun Project.setupStandaloneLint() {
    extensions.configure(Lint::class.java) {
        it.configure(project)
    }
}

internal fun Project.configureCommonAndroid() {
    android {
        namespace = pathBasedAndroidNamespace()
        compileSdk = getVersion("android-compile").toInt()
        defaultConfig.minSdk = getVersion("android-min").toInt()
        lint.configure(project)
    }
}

@Suppress("UnstableApiUsage")
internal fun KotlinMultiplatformAndroidLibraryTarget.configureCommonAndroid(project: Project) {
    namespace = project.pathBasedAndroidNamespace()
    compileSdk = project.getVersion("android-compile").toInt()
    minSdk = project.getVersion("android-min").toInt()
    lint.configure(project)
}

internal fun Lint.configure(project: Project) {
    lintConfig = project.rootProject.file("gradle/lint.xml")

    checkReleaseBuilds = false
    checkGeneratedSources = false
    checkTestSources = false
    checkDependencies = true
    ignoreTestSources = true
    abortOnError = true
    warningsAsErrors = true

    // Temporary workaround: Compose lint detector ComposableFlowOperatorDetector crashes with Kotlin metadata 2.1.0
    // Disable the specific check until Compose lint supports metadata 2.1.0
    // See build failure message suggesting: disable "FlowOperatorInvokedInComposition"
    disable.addAll(
        listOf(
            "FlowOperatorInvokedInComposition",
            "StateFlowValueCalledInComposition",
        ),
    )

    htmlReport = true
    htmlOutput = project.reportsFile("lint-result.html").get().asFile
    textReport = true
    textOutput = project.reportsFile("lint-result.txt").get().asFile

    project.dependencies.addIfNotNull("lintChecks", project.getBundleDependenciesOrNull("lint"))
}

private fun Project.reportsFile(name: String): Provider<RegularFile> {
    val projectName = project.path
        .replace("projects", "")
        .replaceFirst(":", "")
        .replace(":", "/")

    return rootProject.layout.buildDirectory.file("reports/lint/$projectName/$name")
}
