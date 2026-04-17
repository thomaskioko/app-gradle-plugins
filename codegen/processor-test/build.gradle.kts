import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(libs.versions.java.toolchain.get().toInt())
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.java.target.get()))
    }
}

java {
    targetCompatibility = JavaVersion.toVersion(libs.versions.java.target.get())
    sourceCompatibility = JavaVersion.toVersion(libs.versions.java.target.get())
}

dependencies {
    testImplementation(project(":codegen-processor"))
    testRuntimeOnly(project(":codegen-annotations"))
    testImplementation("dev.zacsweers.kctfork:ksp:0.12.1")
    testImplementation("dev.zacsweers.kctfork:core:0.12.1")
    testImplementation(libs.junit)
    testImplementation(libs.ksp.api)
    testImplementation(libs.kotlin.compiler.embeddable)
}

tasks.named<Test>("test") {
    jvmArgs(
        "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
    )
    val updateGoldens =
        providers.gradleProperty("golden.update").orNull
            ?: providers.systemProperty("golden.update").orNull
    if (updateGoldens == "true") {
        systemProperty("golden.update", "true")
    }
    if (System.getenv("GOLDEN_UPDATE") == "true") {
        environment("GOLDEN_UPDATE", "true")
    }
}
