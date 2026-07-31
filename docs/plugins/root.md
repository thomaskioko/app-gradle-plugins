# Root

`io.github.thomaskioko.gradle.plugins.root`

Apply this on the root project before anything else. Every other plugin in the suite checks for it
and fails at once with a clear message if it is missing.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.root")
}
```

It refuses to be applied to a module, so a misplaced `id(...)` fails rather than half working.

## What it does

- Registers the aggregate test tasks `linuxTest`, `iosTest` and `ciTest`, which the module plugins
  attach their own test tasks to
- Sets the Java vendor used for the Gradle daemon toolchain
- Applies dependency analysis at the root, so `buildHealth` covers the whole build, and sets the
  severity of each issue category
- Creates the `moduleGraph {}` block described below
- Configures Gradle Doctor, if the project applies it

## moduleGraph

Generates a diagram of how the modules depend on each other. `graphDump` writes it, `graphUpdate`
rewrites the copy under version control.

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
