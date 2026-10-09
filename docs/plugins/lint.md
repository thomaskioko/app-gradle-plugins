# Lint

`io.github.thomaskioko.gradle.plugins.lint`

This plugin adds the suite's own ktlint rules on top of the standard ones. Apply it on the root
project.

```kotlin
plugins {
    id("io.github.thomaskioko.gradle.plugins.lint")
}
```

## Features

The plugin reads its own version from its jar and asks for the matching `lint-rules` artifact.
Then it hands that to [Spotless](spotless.md) as a custom rule set for the root project and every
module.

The versions can't drift apart. We build the coordinate from the plugin's own version, so moving
the plugin version in your catalog moves the rules with it.

## Rules

The rules cover conventions the type system can't enforce. Navigation is only constructed inside
the modules that own it. Bindings come from the code generation annotations, not from code
written by hand. A test is named after the behaviour it checks. Rules with exemptions read them
from `.editorconfig`. So a module that legitimately breaks a rule says so in the file it applies
to.

Every rule, and the `.editorconfig` property that exempts a module from it, is on the
[lint rules](../lint-rules.md) page. The classes themselves are in the
[API reference](../api/lint-rules/index.html).
