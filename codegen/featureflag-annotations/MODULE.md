# Module featureflag-annotations

Every feature flag added by hand costs a qualifier annotation and two provider functions, one to
build the flag and one to add it to the set the debug screen reads. Three declarations for a
single boolean, and the flag is invisible in the debug screen if the second provider is forgotten.

`@FeatureFlag` replaces all three. Declare the flag once and the processor in
`codegen-featureflag-processor` generates the rest.

```kotlin
@FeatureFlag(
  key = "enable_continue_watching_nitro",
  title = "Progress Endpoint",
  description = "Use Trakt's progress call instead of the multi-step fetch.",
  defaultValue = false,
  dateAdded = "2026-05-20",
)
object ContinueWatchingNitroFlag
```

Published as `io.github.thomaskioko.gradle.plugins:codegen-featureflag-annotations`.

# Package io.github.thomaskioko.codegen.annotations

`@FeatureFlag` and the `Platform` enumeration that limits a flag to the platforms it applies to.
The annotation's own documentation lists the files it generates and the values the processor
rejects.
