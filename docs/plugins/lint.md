# Lint

`io.github.thomaskioko.gradle.plugins.lint`

Apply this on the root project to add the suite's own ktlint rules on top of the standard ones.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.lint")
}
```

## What it does

It reads its own version from its jar and asks for the matching `lint-rules` artifact, then hands
that to [Spotless](spotless.md) as a custom rule set for the root project and every module.

The versions cannot drift apart. Moving the plugin version in your catalog moves the rules with
it, because the coordinate is built from the version of the plugin doing the asking.

## The rules

The rules cover conventions a type cannot enforce: keeping navigation construction inside the
modules that own it, requiring the code generation annotations rather than bindings written by
hand, and naming a test after the behaviour it checks. Each one reads its exemptions from
`.editorconfig` where it has any, so a module that legitimately breaks a rule says so in the file
it applies to.

Every rule is listed in the
[lint rules API reference](../api/lint-rules/index.html).
