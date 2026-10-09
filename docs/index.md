# App Gradle Plugins

This repository contains Gradle plugins that are used to build Kotlin Multiplatform and Android
projects. They hold the setup every module would otherwise repeat (targets, compiler flags, test
dependencies), so a module's build file only declares the choices that are its own, inside a
`scaffold {}` block.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.multiplatform")
}

scaffold {
    useMetro()
}
```

## Highlights

### Convention plugins

Plugins for Kotlin Multiplatform, Android and JVM modules. You apply the root plugin on the root
project and one platform plugin on each module. Then you describe the module through
`scaffold {}`.

### Code generation

KSP processors for navigation and feature flags. The navigation codegen generates the dependency
graph and bindings for a presenter you mark as a destination. Those are four files you would
otherwise write by hand for every screen.

### Lint rules

ktlint rules for the conventions the type system can't enforce. For example, navigation is
only constructed inside the modules that own it, and a binding comes from the code generation
annotation rather than being written by hand.

## Documentation

The [API reference](api/plugins/index.html) covers every plugin, every option in `scaffold {}`, and every
annotation. We generate it from the source, so it can't drift away from the code.

[Installing](install.md) walks through the settings a project needs before the first module
builds. We checked each step against an empty project.

The [change log](changelog.md) records what changed in each release. It also says what you have
to do about it, if anything.

## Usage requirements

The plugins are built against AGP 9 and current Kotlin, and published to Maven Central. We use
them in production in [Tv Maniac](https://github.com/c0de-wizard/tv-maniac). Most of the
requirements come from there.
