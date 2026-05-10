package io.github.thomaskioko.codegen.processor.data

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName

/**
 * Structured representation of an `@AppRoot` annotated presenter implementation. Produced by
 * [io.github.thomaskioko.codegen.processor.parser.parseAppRootData] and consumed by
 * [io.github.thomaskioko.codegen.processor.codegen.AppRootBindingGenerator].
 *
 * The generator emits a `@BindingContainer @ContributesTo(parentScope) object` named
 * `<InterfaceName>BindingContainer` containing one `@Provides @SingleIn(parentScope)` function
 * that takes a `ComponentContext` and the nested factory, and returns the bound interface.
 *
 * @property implClassName The annotated `@AssistedInject` implementation class.
 * @property interfaceClassName The single non-marker supertype the implementation extends. The
 *   generated provider returns this type.
 * @property factoryClassName The nested `@AssistedFactory` interface inside [implClassName]. The
 *   generated provider takes this as a parameter and calls its factory function.
 * @property factoryFunctionName The name of the factory function inside [factoryClassName] that
 *   takes a `ComponentContext` and returns the implementation.
 * @property parentScope The parent dependency injection scope hosting the generated binding
 *   (typically `ActivityScope`).
 * @property packageName The package the generated binding lands in. Equals the implementation's
 *   package plus a `.di` suffix.
 */
internal data class AppRootData(
    val implClassName: ClassName,
    val interfaceClassName: ClassName,
    val factoryClassName: ClassName,
    val factoryFunctionName: String,
    val parentScope: ClassName,
    val packageName: String,
) {
    val bindingClassName: ClassName =
        ClassName(packageName, "${interfaceClassName.simpleName}BindingContainer")
    val provideFunName: String =
        "provide${interfaceClassName.simpleName}"
}

/**
 * Structured representation of an `@AppRootUi` annotated composable. Produced by
 * [io.github.thomaskioko.codegen.processor.parser.parseAppRootUiData] and consumed by
 * [io.github.thomaskioko.codegen.processor.codegen.AppRootUiBindingGenerator].
 *
 * The generator emits an `AppRootProvider` interface declaring one `val` for each entry in
 * [parameters], plus a `@Composable AppRootProvider.AppRootContent(modifier)` extension that
 * invokes the annotated composable using the receiver's properties.
 *
 * @property composableFunction Reference to the annotated composable. Held as a [MemberName] so
 *   the generated code calls the composable as a top level function through KotlinPoet's `%M`
 *   interpolation.
 * @property packageName The composable's package. The generated binding lands in `$packageName.di`.
 * @property parameters The composable's non-modifier parameters in declaration order. Each becomes
 *   a property on the generated `AppRootProvider` interface.
 * @property hasModifier Whether the composable accepts a `modifier: Modifier` parameter. The
 *   generated extension forwards the receiver's modifier when `true` and omits the parameter
 *   when `false`.
 * @property parentScope The parent dependency injection scope hosting the generated artifacts
 *   (typically `ActivityScope`).
 */
internal data class AppRootUiData(
    val composableFunction: MemberName,
    val packageName: String,
    val parameters: List<AppRootUiParameter>,
    val hasModifier: Boolean,
    val parentScope: ClassName,
) {
    val functionName: String get() = composableFunction.simpleName
    val bindingClassName: ClassName =
        ClassName("$packageName.di", "${functionName}AppRootUiBinding")
    val providerInterfaceName: ClassName =
        ClassName("$packageName.di", "AppRootProvider")
}

/**
 * One non-modifier parameter on an `@AppRootUi` annotated composable. Each becomes a property on
 * the generated `AppRootProvider` interface.
 *
 * @property name Parameter name as written on the composable. Used as both the property name on
 *   the provider interface and the argument name in the generated extension's call to the
 *   composable.
 * @property type Parameter type. Used as the property type on the provider interface.
 */
internal data class AppRootUiParameter(
    val name: String,
    val type: TypeName,
)
