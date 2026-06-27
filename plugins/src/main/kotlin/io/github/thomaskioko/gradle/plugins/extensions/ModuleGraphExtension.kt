package io.github.thomaskioko.gradle.plugins.extensions

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

@ScaffoldDsl
public abstract class ModuleGraphExtension {
    public abstract val ignoredProjectsRegex: Property<String>

    public abstract val ignoredProjects: SetProperty<String>

    public abstract val supportedConfigurations: SetProperty<String>

    init {
        ignoredProjectsRegex.convention(".*:testing$")
        supportedConfigurations.convention(
            setOf("commonMainApi", "commonMainImplementation", "api", "implementation"),
        )
    }

    public fun ignore(vararg projectPaths: String) {
        ignoredProjects.addAll(*projectPaths)
    }
}
