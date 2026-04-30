package io.github.thomaskioko.gradle.plugins.setup

import io.github.thomaskioko.gradle.plugins.utils.addImplementationDependency
import io.github.thomaskioko.gradle.plugins.utils.addKspDependencyForAllTargets
import io.github.thomaskioko.gradle.plugins.utils.getDependency
import org.gradle.api.Project

internal fun Project.setupCodegen() {
    // Codegen relies on Metro's annotation processing; ensure Metro is configured first.
    if (!plugins.hasPlugin("dev.zacsweers.metro")) {
        setupMetro()
    }

    setupKsp()
    addImplementationDependency(getDependency("codegen-annotations"))
    addKspDependencyForAllTargets(getDependency("codegen-processor"))
}
