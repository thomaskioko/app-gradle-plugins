# Spotless

`io.github.thomaskioko.gradle.plugins.spotless`

You do not apply this one. [Base](base.md) applies it, which means every module using this suite
is formatted the same way without asking for it.

## What it does

- Applies Spotless and points ktlint at Kotlin sources under `src`, Kotlin build files, and XML
- Uses the ktlint version from your version catalog, so the formatter moves when you decide it does
- Picks up any custom rule sets registered by the [Lint](lint.md) plugin
- Skips modules named `benchmark`, where formatting adds noise and nothing else

Formatting is deferred until the project is evaluated, so a plugin applied later in the same build
file can still register its rules before ktlint reads them.

## What this means for your build files

The formatter covers your build files as well as your source, using four spaces. A build file
written with two will fail the first `spotlessCheck` and be rewritten by `spotlessApply`.

```bash
./gradlew spotlessApply
```
