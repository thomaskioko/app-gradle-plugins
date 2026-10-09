# Resource generator

`io.github.thomaskioko.gradle.plugins.resource.generator`

This plugin generates typed keys for your translated strings. Apply it on the module that holds
them, if that module uses Moko resources.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.resource.generator")
}
```

## What it does

The plugin registers `generateMokoStrings`. The task reads the string and plural accessors Moko
generates for `commonMain`. It then writes a pair of sealed classes that name every string and
plural key. It reads the layout Moko 0.27.0 introduced, where each key is an extension property
on `MR.strings` or `MR.plurals`. So it needs Moko 0.27.0 or later.

Renaming or removing a key now gives you a compile error. Without it, you'd get a string that
silently fails to resolve at runtime.

The generated sources are added to `commonMain`. The task runs after Moko's own generation, so a
plain build produces them in the right order without extra setup.

```bash
./gradlew generateMokoStrings
```
