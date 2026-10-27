# Root

`io.github.thomaskioko.gradle.plugins.root`

This plugin configures the root project. Apply it there before anything else.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.root")
}
```

It refuses to be applied to a module. A misplaced `id(...)` fails instead of half working.

## Features

- Sets the Java vendor used for the Gradle daemon toolchain
- Applies dependency analysis at the root, so `buildHealth` covers the whole build. It also sets
  the severity of each issue category
- Creates the `moduleGraph {}` block described below
- Configures Gradle Doctor, if the project applies it

## moduleGraph

This block configures a diagram of how the modules depend on each other. `graphDump` writes it.
`graphUpdate` rewrites the copy under version control.

```kotlin
moduleGraph {
    ignore(":benchmark", ":sample")
}
```

| Option | What it does |
|---|---|
| `ignore(vararg projectPaths)` | Leaves the named projects out of the diagram |
| `ignoredProjects` | The same set, as a property |
| `ignoredProjectsRegex` | Leaves out every project whose path matches |
| `supportedConfigurations` | Which configurations count as a dependency edge |

Full detail in the
[API reference](../api/plugins/plugins/io.github.thomaskioko.gradle.plugins.extensions/-module-graph-extension/index.html).
