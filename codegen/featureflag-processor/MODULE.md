# Module featureflag-processor

The symbol processor behind `@FeatureFlag`. It reads each annotated declaration and writes the
qualifier and the binding pair, and reports a compile error when the flag is declared with a blank
key, a blank title or a date that is not a valid `YYYY-MM-DD`.

Nothing here is called from application code. The processor runs under KSP, which the
`useFeatureFlagCodegen()` option in `scaffold {}` sets up.

```kotlin
scaffold {
  useFeatureFlagCodegen()
}
```

Published as `io.github.thomaskioko.gradle.plugins:codegen-featureflag-processor`.

# Package io.github.thomaskioko.codegen.featureflag.processor

The processor entry point and the provider KSP loads it through.
