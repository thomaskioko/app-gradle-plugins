pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("dependency-config")
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

rootProject.name = "app-gradle-plugins"

includeBuild("codegen")
includeBuild("plugins")
includeBuild("lint-rules")
