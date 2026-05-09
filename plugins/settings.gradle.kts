pluginManagement {
    includeBuild("../build-logic")
}

plugins {
    id("dependency-config")
    id("publishing-config")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "plugins"
