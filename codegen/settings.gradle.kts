pluginManagement {
    includeBuild("../build-logic")
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.0")
    }
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

rootProject.name = "codegen"

include(
    ":annotations",
    ":featureflag-annotations",
    ":processor",
    ":featureflag-processor",
    ":processor-test",
)
