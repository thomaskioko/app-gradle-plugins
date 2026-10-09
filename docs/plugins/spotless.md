# Spotless

`io.github.thomaskioko.gradle.plugins.spotless`

This plugin formats every module in the suite the same way. You don't apply it yourself.
[Base](base.md) applies it for you.

## What it does

- Applies Spotless and points ktlint at Kotlin sources under `src`, Kotlin build files, and XML
- Uses the ktlint version from your version catalog, so the formatter only moves when you move it
- Picks up any custom rule sets registered by the [Lint](lint.md) plugin
- Skips modules named `benchmark`, where formatting adds noise and nothing else

We defer formatting until the project is evaluated. That way a plugin applied later in the same
build file can still register its rules before ktlint reads them.

## What this means for your build files

The formatter covers your build files as well as your source, with four spaces. A build file
written with two fails the first `spotlessCheck`. `spotlessApply` rewrites it.

```bash
./gradlew spotlessApply
```
