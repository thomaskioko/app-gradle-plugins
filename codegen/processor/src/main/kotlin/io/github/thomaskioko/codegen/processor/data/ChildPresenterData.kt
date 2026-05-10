package io.github.thomaskioko.codegen.processor.data

import com.squareup.kotlinpoet.ClassName

/**
 * Structured representation of a `@ChildPresenter` annotated presenter class. Produced by
 * [io.github.thomaskioko.codegen.processor.parser.parseChildPresenterData] and consumed by
 * [io.github.thomaskioko.codegen.processor.codegen.ChildGraphGenerator].
 *
 * The generator emits a `@GraphExtension(scope) interface <Presenter>ChildGraph` exposing the
 * presenter as a property and a `@ContributesTo(parentScope) @GraphExtension.Factory` nested
 * interface that takes a `ComponentContext` and returns the graph.
 *
 * @property presenterClass The presenter class the annotation was attached to.
 * @property baseName The presenter class name with the `Presenter` suffix stripped.
 * @property packageName Package the generated file lands in (the presenter's package plus `.di`).
 * @property scope Graph scope (typically a marker abstract class shared by sibling child
 *   presenters under the same host).
 * @property parentScope Parent dependency injection scope the generated factory contributes to.
 */
internal data class ChildPresenterData(
    val presenterClass: ClassName,
    val baseName: String,
    val packageName: String,
    val scope: ClassName,
    val parentScope: ClassName,
) {
    val graphClassName: ClassName = ClassName(packageName, "${baseName}ChildGraph")
    val graphFactoryFunName: String = "create${baseName}Graph"
    val graphPropertyName: String = baseName.replaceFirstChar { it.lowercaseChar() } + "Presenter"
}
