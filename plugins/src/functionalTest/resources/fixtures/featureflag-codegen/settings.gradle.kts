pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

// Consume the plugin and codegen as a composite build (from source), so the functional test needs
// no published version. Both build roots are injected by the functionalTest task (see FixtureProject
// and plugins/build.gradle.kts). Including the plugin build resolves the convention plugin ids by
// id, which is why this fixture does not use withPluginClasspath. The codegen substitution is
// explicit because the published artifactId differs from the project name.
providers.gradleProperty("pluginsDir").orNull?.let { includeBuild(it) }
providers.gradleProperty("codegenDir").orNull?.let { dir ->
    includeBuild(dir) {
        dependencySubstitution {
            substitute(module("io.github.thomaskioko.gradle.plugins:codegen-featureflag-annotations"))
                .using(project(":featureflag-annotations"))
            substitute(module("io.github.thomaskioko.gradle.plugins:codegen-featureflag-processor"))
                .using(project(":featureflag-processor"))
        }
    }
}

rootProject.name = "fixture-featureflag-codegen"

include(":feature")
