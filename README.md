# App Gradle Plugins

[![Maven Central](https://img.shields.io/maven-central/v/io.github.thomaskioko.gradle.plugins/plugins)](https://central.sonatype.com/artifact/io.github.thomaskioko.gradle.plugins/plugins)
[![Build](https://github.com/thomaskioko/app-gradle-plugins/actions/workflows/build.yml/badge.svg)](https://github.com/thomaskioko/app-gradle-plugins/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

A module in a large project spends most of its build file repeating the same setup: the same
target platforms, the same compiler flags, the same test dependencies. These plugins move that
setup out of the build file and leave behind a `scaffold {}` block where a module declares only
the choices it actually has to make.

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

Alongside the plugins are a KSP code generator that writes the navigation graph for an annotated
presenter, and a ktlint rule set for the conventions a type cannot enforce.

## Documentation

**<https://thomaskioko.github.io/app-gradle-plugins/>**

- [Installing](https://thomaskioko.github.io/app-gradle-plugins/install/) covers what a project
  needs before the first module builds. Each step was checked against an empty project.
- [Plugins](https://thomaskioko.github.io/app-gradle-plugins/plugins/) has a page for each of the
  eleven plugins and every option in `scaffold {}`.
- [Navigation](https://thomaskioko.github.io/app-gradle-plugins/navigation/get-started/) covers the
  annotations that generate a screen's graph and bindings.
- [Feature flags](https://thomaskioko.github.io/app-gradle-plugins/feature-flags/) covers declaring
  a flag once and having its qualifier and bindings written for you.
- [Lint rules](https://thomaskioko.github.io/app-gradle-plugins/lint-rules/) lists every rule and
  how to exempt a module from one.
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
