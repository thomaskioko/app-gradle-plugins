apply(from = "../gradle/repositories.settings.gradle.kts")
apply(from = "../gradle/publishing.settings.gradle.kts")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "codegen-plugins"

include(
    ":annotations",
    ":processor",
    ":processor-test",
)

project(":annotations").name = "codegen-annotations"
project(":processor").name = "codegen-processor"
project(":processor-test").name = "codegen-processor-test"
