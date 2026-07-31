# App Gradle Plugins

A module in a large project spends most of its build file repeating the same setup: the same
target platforms, the same compiler flags, the same test dependencies. These plugins move that
setup out of the build file and leave behind a `scaffold {}` block where a module declares only
the choices it actually has to make.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.multiplatform")
}

scaffold {
    useMetro()
}
```

## What is here

**Convention plugins** for Kotlin Multiplatform, Android and JVM modules. Apply the root plugin on
the root project and one platform plugin on each module, then describe the module through
`scaffold {}`.

**Code generation** for navigation and feature flags. Mark a presenter as a destination and the
processor writes the dependency graph and the bindings that go with it, instead of leaving four
mechanical files to be written by hand for every screen.

**ktlint rules** for the conventions a type cannot enforce, such as keeping navigation
construction inside the modules that own it, or requiring the code generation annotation rather
than a binding written by hand.

## Where to go next

The [API reference](api/plugins/index.html) covers every plugin, every option in `scaffold {}`, and every
annotation, generated from the source itself so it cannot drift away from the code.

[Installing](install.md) walks through the settings a project needs before the first module
builds, each step checked against an empty project.

The [change log](changelog.md) records what changed in each release and what a consumer has to do
about it, if anything.

## Compatibility

These plugins are built against AGP 9 and current Kotlin. They are published to Maven Central and
used in production by [Tv Maniac](https://github.com/c0de-wizard/tv-maniac), which is where most
of the requirements come from.
