package io.github.thomaskioko.gradle.plugins.extensions

/**
 * DSL marker for the `scaffold {}` extension hierarchy.
 *
 * Prevents nested DSL blocks (e.g. `scaffold { android { … } }`) from accidentally calling
 * methods on an outer-scope receiver.
 */
@DslMarker
public annotation class ScaffoldDsl
