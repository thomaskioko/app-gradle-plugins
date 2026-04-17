package io.github.thomaskioko.codegen.annotations

import kotlin.reflect.KClass

/**
 * Marks a presenter class as a home-tab destination.
 *
 * The navigation codegen processor generates a `TabDestination` contribution that creates
 * a `TabChild<presenter>` for the matching config. Unlike [NavScreen], no `NavRouteBinding`
 * is generated because the host tab feature owns its own config serialization. The [config]
 * class itself is used as the generated graph's scope marker, which keeps the scope visible
 * from every consumer (including `commonMain` and sibling modules) without a separately
 * generated class.
 *
 * @property config the config subtype this tab matches (usually a data object). Also used as
 *                  the generated graph's scope marker.
 * @property parentScope the parent DI scope. Typically the host route class (e.g.
 *                       `HomeRoute::class`) so that tabs contribute into the same graph that
 *                       owns `TabDestination` multibinding.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class TabScreen(
    val config: KClass<*>,
    val parentScope: KClass<*>,
)
