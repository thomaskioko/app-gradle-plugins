apply(from = "../gradle/repositories.settings.gradle.kts")
apply(from = "../gradle/publishing.settings.gradle.kts")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "plugins"
