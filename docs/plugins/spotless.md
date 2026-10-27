# Spotless

`io.github.thomaskioko.gradle.plugins.spotless`

This plugin formats every module in the suite the same way. [Base](base.md) applies it to every
module. Apply it on the root project as well if you want the root build files formatted.

## Features

- Applies Spotless and points ktlint at Kotlin sources under `src`, Kotlin build files, and XML
- Uses the ktlint version from your version catalog, so the formatter only moves when you move it
- Adds the suite's own rules when `app.enableLintRules=true`, see [Lint](lint.md)
- Adds any rule sets the module's build file lists in its `app.spotless.customRuleSets` extra
  property
- Skips modules named `benchmark`, where formatting adds noise and nothing else

We defer formatting until the project is evaluated. That way the build file can still register
its rules before ktlint reads them.

## Build files

The formatter covers your build files as well as your source, with four spaces. A build file
written with two fails the first `spotlessCheck`. `spotlessApply` rewrites it.

```bash
./gradlew spotlessApply
```
