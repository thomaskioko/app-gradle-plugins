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
    dependsOn(gradle.includedBuild("codegen").task(":annotations:publishToMavenLocal"))
    dependsOn(gradle.includedBuild("codegen").task(":processor:publishToMavenLocal"))
    dependsOn(gradle.includedBuild("lint-rules").task(":publishToMavenLocal"))
}

tasks.register("buildHealthAll") {
    group = "verification"
    description = "Run buildHealth across plugins + lint-rules + codegen composite builds."
    dependsOn(gradle.includedBuild("plugins").task(":buildHealth"))
    dependsOn(gradle.includedBuild("lint-rules").task(":buildHealth"))
    dependsOn(gradle.includedBuild("codegen").task(":buildHealth"))
}

tasks.register("spotlessApplyAll") {
    group = "formatting"
    description = "Run spotlessApply across root + plugins + lint-rules + codegen composite builds."
    dependsOn("spotlessApply")
    dependsOn(gradle.includedBuild("plugins").task(":spotlessApply"))
    dependsOn(gradle.includedBuild("lint-rules").task(":spotlessApply"))
    dependsOn(gradle.includedBuild("codegen").task(":spotlessApply"))
}

tasks.register("spotlessCheckAll") {
    group = "verification"
    description = "Run spotlessCheck across root + plugins + lint-rules + codegen composite builds."
    dependsOn("spotlessCheck")
    dependsOn(gradle.includedBuild("plugins").task(":spotlessCheck"))
    dependsOn(gradle.includedBuild("lint-rules").task(":spotlessCheck"))
    dependsOn(gradle.includedBuild("codegen").task(":spotlessCheck"))
}
