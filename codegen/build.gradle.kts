import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.publish) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.dependency.analysis)
}

val ktlintVersion = libs.versions.ktlint.get()
val licenseHeader = rootProject.file("../spotless/spotless.kt")
val ktsLicenseDelimiter = "(import|plugins|pluginManagement|dependencyResolutionManagement)"

spotless {
    kotlinGradle {
        ktlint(ktlintVersion)
        target("*.kts")
        licenseHeaderFile(licenseHeader, ktsLicenseDelimiter)
    }
}

subprojects {
    group = property("GROUP").toString()
    version = property("VERSION_NAME").toString()

    pluginManager.apply("com.autonomousapps.dependency-analysis")
    pluginManager.apply("com.diffplug.spotless")

    configure<SpotlessExtension> {
        kotlin {
            ktlint(ktlintVersion).editorConfigOverride(mapOf("android" to "true"))
            target("src/**/*.kt")
            targetExclude("**/resources/**", "**/build/**")
            licenseHeaderFile(licenseHeader)
        }
        kotlinGradle {
            ktlint(ktlintVersion)
            target("*.kts")
            licenseHeaderFile(licenseHeader, ktsLicenseDelimiter)
        }
    }
}
