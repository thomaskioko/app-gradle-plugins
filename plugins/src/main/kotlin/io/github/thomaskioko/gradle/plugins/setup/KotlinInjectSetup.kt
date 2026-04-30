package io.github.thomaskioko.gradle.plugins.setup

import io.github.thomaskioko.gradle.plugins.utils.addBundleImplementationDependency
import io.github.thomaskioko.gradle.plugins.utils.addKspDependencyForAllTargets
import io.github.thomaskioko.gradle.plugins.utils.getBundleDependencies
import io.github.thomaskioko.gradle.plugins.utils.getDependency
import org.gradle.api.Project

internal fun Project.setupKotlinInject() {
    setupKsp()

    addBundleImplementationDependency(getBundleDependencies("kotlinInject"))
    addKspDependencyForAllTargets(getDependency("kotlinInject-compiler"))
    addKspDependencyForAllTargets(getDependency("kotlinInject-anvil-compiler"))
}
