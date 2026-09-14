# Resource generator

`io.github.thomaskioko.gradle.plugins.resource.generator`

Apply this on the module holding your translated strings, if that module uses Moko resources.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.resource.generator")
}
```

## What it does

Registers `generateMokoStrings`, which reads the string and plural accessors Moko generates for
`commonMain` and writes a pair of sealed classes naming every string and plural key. It reads the
layout Moko 0.27.0 introduced, where each key is an extension property on `MR.strings` or
`MR.plurals`, so it needs Moko 0.27.0 or later. The result is a compile error when a key is
renamed or removed, rather than a string that silently fails to resolve at runtime.

The generated sources are added to `commonMain`, and the task is chained after Moko's own
generation, so a plain build produces them in the right order without being told to.

```bash
./gradlew generateMokoStrings
```
