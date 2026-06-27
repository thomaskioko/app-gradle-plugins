package io.github.thomaskioko.codegen.processor.data

import com.squareup.kotlinpoet.ClassName

/**
 * Structured representation of a `@ChildPresenter` annotated presenter class. Produced by
 * [io.github.thomaskioko.codegen.processor.parser.parseChildPresenterData] and consumed by
 * [io.github.thomaskioko.codegen.processor.codegen.ChildGraphGenerator].
 *
 * The generator emits a `@GraphExtension(scope) interface <Presenter>ChildGraph` exposing the
 * presenter (or its assisted factory for parameterized children) as a property and a
 * `@ContributesTo(parentScope) @GraphExtension.Factory` nested interface that takes a
 * `ComponentContext` and returns the graph.
 *
 * If the presenter uses plain `@Inject`, [factory] is `null` and the generated graph exposes the
 * presenter directly. If the presenter uses `@AssistedInject` with a nested `@AssistedFactory`
 * (because the parent passes a runtime value such as a show id), [factory] holds the factory's
 * class name and the generated graph exposes that factory instead; the parent calls `create(...)`
 * on it with the runtime arguments. This mirrors the parameterized `@NavDestination` screen path.
 *
 * @property presenterClass The presenter class the annotation was attached to.
 * @property baseName The presenter class name with the `Presenter` suffix stripped.
 * @property packageName Package the generated file lands in (the presenter's package plus `.di`).
 * @property scope Graph scope (typically a marker abstract class shared by sibling child
 *   presenters under the same host).
 * @property parentScope Parent dependency injection scope the generated factory contributes to.
 * @property factory The presenter's nested `@AssistedFactory` class name when the presenter is
 *   parameterized, or `null` for plain `@Inject` presenters.
 */
internal data class ChildPresenterData(
    val presenterClass: ClassName,
    val baseName: String,
    val packageName: String,
    val scope: ClassName,
    val parentScope: ClassName,
    val factory: ClassName? = null,
) {
    val isParameterized: Boolean
        get() = factory != null
    val graphClassName: ClassName = ClassName(packageName, "${baseName}ChildGraph")
    val graphFactoryFunName: String = "create${baseName}Graph"
    val graphPropertyType: ClassName = factory ?: presenterClass
    val graphPropertyName: String =
        if (factory != null) {
            baseName.replaceFirstChar { it.lowercaseChar() } + "Factory"
        } else {
            baseName.replaceFirstChar { it.lowercaseChar() } + "Presenter"
        }
}
