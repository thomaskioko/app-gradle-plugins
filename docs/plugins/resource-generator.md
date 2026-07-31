# Resource generator

`io.github.thomaskioko.gradle.plugins.resource.generator`

Apply this on the module holding your translated strings, if that module uses Moko resources.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.resource.generator")
}
```

## What it does

Registers `generateMokoStrings`, which reads the `MR` object Moko generates and writes a pair of
sealed classes naming every string and plural key. The result is a compile error when a key is
renamed or removed, rather than a string that silently fails to resolve at runtime.

The generated sources are added to `commonMain`, and the task is chained after Moko's own
generation, so a plain build produces them in the right order without being told to.

```bash
./gradlew generateMokoStrings
```
