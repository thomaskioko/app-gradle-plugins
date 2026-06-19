package io.github.thomaskioko.codegen.annotations

/**
 * Restricts which platform graph a [FeatureFlag] is generated into.
 *
 * [ALL] (the default) generates the qualifier and binding for every platform, so the flag appears on
 * both the Android and iOS debug screens. [IOS] and [JVM] scope generation to one platform at
 * compile time: the binding contributes only to that platform's Metro graph, and the other
 * platform's binary never contains the code. The anchor declaration stays in `commonMain` either
 * way; this field is the only control.
 *
 */
public enum class Platform {
    ALL,
    IOS,
    JVM,
}
