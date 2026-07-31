# Module plugins

A module in a large project spends most of its build file repeating the same setup: the same
target platforms, the same compiler flags, the same test dependencies. These plugins move that
setup out of the build file and leave behind a `scaffold {}` block where a module declares only
the choices it actually has to make.

Apply the root plugin on the root project, then one plugin per module.

```kotlin
// root build.gradle.kts
plugins {
  id("io.github.thomaskioko.gradle.plugins.root")
}
```

```kotlin
// a module's build.gradle.kts
plugins {
  id("io.github.thomaskioko.gradle.plugins.multiplatform")
}

scaffold {
  useMetro()
}
```

The root plugin registers the aggregate test tasks, sets the daemon toolchain vendor, configures
dependency analysis and the module graph tasks, and checks that it was applied to the root project
and not a module. Every module plugin checks that the root plugin is present and fails with a clear
message when it is not.

# Package io.github.thomaskioko.gradle.plugins

The plugin entry points. Each one is registered under an `io.github.thomaskioko.gradle.plugins.*`
identifier and is applied from a build file.

- `root` on the root project, before anything else
- `app`, `android`, `jvm` or `multiplatform` on a module, one of the four
- `base` when a module needs the shared setup without a platform
- `baseline.profile`, `resource.generator` and `buildconfig` for the modules that need them

# Package io.github.thomaskioko.gradle.plugins.extensions

The `scaffold {}` block and the platform blocks nested inside it. This is where a module says what
it is: which platforms it targets, whether it uses Compose, which dependency injection and code
generation it needs, and which dependencies to hold to a baseline.

# Package io.github.thomaskioko.gradle.plugins.checks

Static analysis applied to a module. `SpotlessPlugin` configures Spotless with the project's ktlint
setup, and `LintPlugin` adds the custom rule set from the `lint-rules` artifact so those rules run
alongside the standard ones.
