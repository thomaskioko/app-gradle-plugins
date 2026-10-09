# App Gradle Plugins

[![Maven Central](https://img.shields.io/maven-central/v/io.github.thomaskioko.gradle.plugins/plugins)](https://central.sonatype.com/artifact/io.github.thomaskioko.gradle.plugins/plugins)
[![Build](https://github.com/thomaskioko/app-gradle-plugins/actions/workflows/build.yml/badge.svg)](https://github.com/thomaskioko/app-gradle-plugins/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

This repository contains Gradle plugins that are used to build Kotlin Multiplatform and Android
projects. They hold the setup every module would otherwise repeat (targets, compiler flags, test
dependencies), so a module's build file only declares the choices that are its own, inside a
`scaffold {}` block.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.multiplatform")
}

scaffold {
    addJvmTarget()
    addIosTargets()
    addAndroidTarget()

    useMetro()
}
```

The repository also contains a KSP code generator and a ktlint rule set. The code generator
writes the navigation graph for an annotated presenter. The rules cover conventions the type
system can't enforce.

## Documentation

**<https://thomaskioko.github.io/app-gradle-plugins/>**

- [Installing](https://thomaskioko.github.io/app-gradle-plugins/install/) covers what a project
  needs before the first module builds. We checked each step against an empty project.
- [Plugins](https://thomaskioko.github.io/app-gradle-plugins/plugins/) has a page for each of the
  eleven plugins. It also covers every option in `scaffold {}`.
- [Navigation](https://thomaskioko.github.io/app-gradle-plugins/navigation/get-started/) covers the
  annotations that generate a screen's graph and bindings.
- [Feature flags](https://thomaskioko.github.io/app-gradle-plugins/feature-flags/) covers declaring
  a flag once. The codegen writes its qualifier and bindings for you.
- [Lint rules](https://thomaskioko.github.io/app-gradle-plugins/lint-rules/) lists every rule, and
  how you exempt a module from one.
- [API reference](https://thomaskioko.github.io/app-gradle-plugins/api/plugins/) is generated from
  the source.

## Building

```bash
./gradlew build            # the root build
./gradlew spotlessApplyAll # format everything
./gradlew buildHealthAll   # check dependencies
./gradlew dokkaAll         # build the API reference
./deploy_website.sh --local # read the site while editing it
```

Releasing is documented in [RELEASING.md](RELEASING.md).

## License

```
Copyright 2025 Thomas Kioko

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
