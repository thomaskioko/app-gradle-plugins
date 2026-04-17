apply(from = "gradle/repositories.settings.gradle.kts")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

rootProject.name = "app-gradle-plugins"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

includeBuild("plugins")
includeBuild("codegen") {
    name = "codegen-plugins"
}
