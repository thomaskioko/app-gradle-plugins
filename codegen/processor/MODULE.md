# Module processor

The symbol processor behind the navigation annotations. It reads each annotated presenter and
composable and writes the graph extension, the destination binding and the route binding that the
annotation promises.

Nothing here is called from application code. The processor runs under KSP, which the
`useCodegen()` option in `scaffold {}` sets up.

```kotlin
scaffold {
  useCodegen()
}
```

This documentation is here for anyone changing how the generated code looks, or working out why a
particular file was emitted. The generated shapes themselves are described on the annotations in
`codegen-annotations`, which is the better starting point when using them rather than changing
them.

Published as `io.github.thomaskioko.gradle.plugins:codegen-processor`.

# Package io.github.thomaskioko.codegen.processor

The processor entry point and the provider KSP loads it through.
