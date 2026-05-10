package io.github.thomaskioko.codegen.annotations

import kotlin.reflect.KClass

/**
 * Marks an `@AssistedInject` presenter implementation as the application's root host, so the
 * codegen processor can wire its bound interface into [parentScope] at compile time.
 *
 * Without this annotation the consumer would have to manually write a Metro `@BindingContainer`
 * that `@Provides @SingleIn(parentScope)` invokes the nested `@AssistedFactory` to instantiate
 * the root presenter. With this annotation the processor emits all of that next to the
 * implementation.
 *
 * ## Example
 *
 * ```kotlin
 * @AppRoot(parentScope = ActivityScope::class)
 * @AssistedInject
 * class DefaultRootPresenter(
 *     @Assisted componentContext: ComponentContext,
 *     // ...
 * ) : RootPresenter, ComponentContext by componentContext {
 *
 *     @AssistedFactory
 *     fun interface Factory {
 *         fun create(componentContext: ComponentContext): DefaultRootPresenter
 *     }
 * }
 * ```
 *
 * The processor emits one file (`<InterfaceName>BindingContainer.kt`) into the implementation's
 * `<package>.di` package. The file contains a `@BindingContainer @ContributesTo(parentScope)
 * object` whose `@Provides @SingleIn(parentScope)` function takes a `ComponentContext` and the
 * nested `Factory`, and returns the bound interface.
 *
 * ## Bound interface inference
 *
 * The processor reads the implementation's supertypes and picks the first non-marker interface as
 * the bound type. Decompose's `ComponentContext` (commonly used as a delegate via
 * `ComponentContext by componentContext`) is filtered out. Implementations that extend more than
 * one non-marker interface are rejected at compile time.
 *
 * ## Validation
 *
 * The processor reports a compile error if any of the following hold:
 *
 * - The annotated symbol is not a class.
 * - The class does not carry `@AssistedInject`.
 * - The class does not declare a nested `@AssistedFactory` interface.
 * - The nested factory does not have exactly one function whose single parameter is a
 *   `ComponentContext` and whose return type equals the implementation type.
 * - The class extends zero or more than one non-marker interface.
 *
 * @property parentScope The dependency injection scope hosting the generated binding (typically
 *   `ActivityScope::class` in the consumer project).
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class AppRoot(
    val parentScope: KClass<*>,
)
