package io.github.thomaskioko.gradle.plugins.setup

import com.dropbox.gradle.plugins.dependencyguard.DependencyGuardPluginExtension
import org.gradle.api.Project

internal fun Project.setupDependencyGuard(vararg configurations: String) {
    plugins.apply("com.dropbox.dependency-guard")
    extensions.configure(DependencyGuardPluginExtension::class.java) { guard ->
        configurations.forEach { guard.configuration(it) }
    }
}
