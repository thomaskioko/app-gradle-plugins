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
    dependsOn(gradle.includedBuild("codegen").task(":codegen-annotations:publishToMavenLocal"))
    dependsOn(gradle.includedBuild("codegen").task(":codegen-processor:publishToMavenLocal"))
}
