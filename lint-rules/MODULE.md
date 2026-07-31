# Module lint-rules

Some conventions cannot be enforced by a type: that navigation is only constructed inside the
modules that own it, that a presenter carries the code generation annotation instead of a binding
written by hand, that a test is named after the behaviour it checks. These are the ktlint rules
that catch those in review instead of leaving them to a reviewer.

The rules are loaded through a ktlint rule set provider, so nothing here is called directly. Add
the artifact to Spotless as a custom rule set, or apply the lint plugin and let it do that.

```kotlin
plugins {
  id("io.github.thomaskioko.gradle.plugins.lint")
}
```

The rules that need an escape hatch read their exemptions from `.editorconfig`, so a module that
legitimately breaks one says so in the file it applies to rather than disabling the rule
everywhere.

# Package io.github.thomaskioko.gradle.plugins.lint

The rule set provider ktlint discovers, and the identifier the rules are registered under.

# Package io.github.thomaskioko.gradle.plugins.lint.navigation

Rules that keep navigation construction and routing inside the modules meant to own them, and that
stop a project from growing a second navigator interface alongside the canonical one.

# Package io.github.thomaskioko.gradle.plugins.lint.codegen

Rules that require the code generation annotations rather than the bindings a developer would
otherwise write by hand, since the two drift apart the moment one of them changes.

# Package io.github.thomaskioko.gradle.plugins.lint.metro

Rules for Metro dependency injection annotations, mainly the ones that are already implied by
another annotation on the same declaration.

# Package io.github.thomaskioko.gradle.plugins.lint.preview

Rules for Compose preview functions, which are easy to write in a way that renders correctly in
the preview and nowhere else.

# Package io.github.thomaskioko.gradle.plugins.lint.tests

Rules for test naming.
