package io.github.thomaskioko.codegen.processor.util

import com.squareup.kotlinpoet.ClassName

/*
 * Fully qualified `ClassName` constants for every type the generated code references in the
 * consumer project. Used by every generator so the names live in one place.
 *
 * These are constants rather than configurable processor options. The full rationale is in
 * `codegen/docs/architecture/consumer-contract.md`. The short version: a fork that changes the
 * consumer's navigation primitives edits this file and the matching test stubs in
 * `processor-test/`. The constants are deliberately `internal` and impossible to override without
 * a fork, which keeps the upstream processor opinionated and small.
 *
 * The groupings below match the role each name plays at runtime.
 */

// Decompose: the navigation library the consumer hosts presenters with. The codegen references
// only ComponentContext, which the generated graph factory function takes as @Provides so every
// presenter on the graph can request it.
internal const val DECOMPOSE_PACKAGE: String = "com.arkivanov.decompose"
internal val ComponentContext: ClassName = ClassName(DECOMPOSE_PACKAGE, "ComponentContext")

// Metro: the dependency injection framework the codegen targets. Every generated annotation
// references one of these. BindingContainer is used only by UiBindingGenerator; see the rationale
// on that generator for why.
internal const val METRO_PACKAGE: String = "dev.zacsweers.metro"
internal val ContributesTo: ClassName = ClassName(METRO_PACKAGE, "ContributesTo")
internal val GraphExtension: ClassName = ClassName(METRO_PACKAGE, "GraphExtension")
internal val Provides: ClassName = ClassName(METRO_PACKAGE, "Provides")
internal val IntoSet: ClassName = ClassName(METRO_PACKAGE, "IntoSet")
internal val BindingContainer: ClassName = ClassName(METRO_PACKAGE, "BindingContainer")
internal val SingleIn: ClassName = ClassName(METRO_PACKAGE, "SingleIn")

// Consumer navigation primitives: route supertypes (NavRoute, NavRoot, BaseRoute), the
// NavDestination sealed family the bindings contribute to, the route binding multibinding
// entries, and the slot child wrappers the factory lambdas produce.
internal const val NAVIGATION_PACKAGE: String = "com.thomaskioko.tvmaniac.navigation"
internal val NavRouteBinding: ClassName = ClassName(NAVIGATION_PACKAGE, "NavRouteBinding")
internal val NavRootBinding: ClassName = ClassName(NAVIGATION_PACKAGE, "NavRootBinding")
internal val NavDestination: ClassName = ClassName(NAVIGATION_PACKAGE, "NavDestination")
internal val NavDestinationScreen: ClassName = NavDestination.nestedClass("Screen")
internal val NavDestinationOverlay: ClassName = NavDestination.nestedClass("Overlay")
internal val NavDestinationTabRoot: ClassName = NavDestination.nestedClass("TabRoot")
internal val ScreenDestination: ClassName = ClassName(NAVIGATION_PACKAGE, "ScreenDestination")
internal val SheetDestination: ClassName = ClassName(NAVIGATION_PACKAGE, "SheetDestination")

// Consumer UI primitives: the multibinding entry types UiBindingGenerator returns. Their
// constructor signatures (matches predicate plus content lambda) are part of the contract;
// changing them in the consumer breaks every generated UI binding.
internal const val NAVIGATION_UI_PACKAGE: String = "com.thomaskioko.tvmaniac.navigation.ui"
internal val ScreenContent: ClassName = ClassName(NAVIGATION_UI_PACKAGE, "ScreenContent")
internal val SheetContent: ClassName = ClassName(NAVIGATION_UI_PACKAGE, "SheetContent")

// Compose: only UiBindingGenerator references Modifier indirectly (through the consumer's
// ScreenContent lambda type). AppRootUiBindingGenerator emits the @Composable annotation and
// the Modifier parameter directly, so it references both ClassName constants below.
internal const val COMPOSE_UI_PACKAGE: String = "androidx.compose.ui"
internal const val COMPOSE_RUNTIME_PACKAGE: String = "androidx.compose.runtime"
internal val Composable: ClassName = ClassName(COMPOSE_RUNTIME_PACKAGE, "Composable")
internal val Modifier: ClassName = ClassName(COMPOSE_UI_PACKAGE, "Modifier")

// Consumer home navigation: TabChild is the wrapper the tab root factory lambda always wraps its
// produced presenter in.
internal const val HOME_NAV_PACKAGE: String = "com.thomaskioko.tvmaniac.home.nav"
internal val TabChild: ClassName = ClassName(HOME_NAV_PACKAGE, "TabChild")
