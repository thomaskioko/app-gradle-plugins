plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.publish) apply false

    alias(libs.plugins.app.root)
    alias(libs.plugins.spotless)
    alias(libs.plugins.app.spotless)
}

tasks.register("publishLocal") {
    group = "publishing"
    description = "Publish plugins + codegen artifacts to mavenLocal."
    dependsOn(gradle.includedBuild("plugins").task(":publishToMavenLocal"))
    dependsOn(gradle.includedBuild("codegen-plugins").task(":codegen-annotations:publishToMavenLocal"))
    dependsOn(gradle.includedBuild("codegen-plugins").task(":codegen-processor:publishToMavenLocal"))
}

tasks.register("buildHealthAll") {
    group = "verification"
    description = "Run buildHealth across plugins + codegen composite builds."
    dependsOn(gradle.includedBuild("plugins").task(":buildHealth"))
    dependsOn(gradle.includedBuild("codegen-plugins").task(":buildHealth"))
}
