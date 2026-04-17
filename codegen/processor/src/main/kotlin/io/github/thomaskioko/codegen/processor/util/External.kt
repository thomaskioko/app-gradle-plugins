package io.github.thomaskioko.codegen.processor.util

import com.squareup.kotlinpoet.ClassName

internal const val DECOMPOSE_PACKAGE: String = "com.arkivanov.decompose"
internal val ComponentContext: ClassName = ClassName(DECOMPOSE_PACKAGE, "ComponentContext")

internal const val METRO_PACKAGE: String = "dev.zacsweers.metro"
internal val ContributesTo: ClassName = ClassName(METRO_PACKAGE, "ContributesTo")
internal val GraphExtension: ClassName = ClassName(METRO_PACKAGE, "GraphExtension")
internal val Provides: ClassName = ClassName(METRO_PACKAGE, "Provides")
internal val IntoSet: ClassName = ClassName(METRO_PACKAGE, "IntoSet")

internal const val NAVIGATION_PACKAGE: String = "com.thomaskioko.tvmaniac.navigation"
internal val NavRoute: ClassName = ClassName(NAVIGATION_PACKAGE, "NavRoute")
internal val NavRouteBinding: ClassName = ClassName(NAVIGATION_PACKAGE, "NavRouteBinding")
internal val NavDestination: ClassName = ClassName(NAVIGATION_PACKAGE, "NavDestination")
internal val RootChild: ClassName = ClassName(NAVIGATION_PACKAGE, "RootChild")
internal val ScreenDestination: ClassName = ClassName(NAVIGATION_PACKAGE, "ScreenDestination")
internal val SheetChild: ClassName = ClassName(NAVIGATION_PACKAGE, "SheetChild")
internal val SheetDestination: ClassName = ClassName(NAVIGATION_PACKAGE, "SheetDestination")
internal val SheetConfig: ClassName = ClassName(NAVIGATION_PACKAGE, "SheetConfig")
internal val SheetChildFactory: ClassName = ClassName(NAVIGATION_PACKAGE, "SheetChildFactory")
internal val SheetConfigBinding: ClassName = ClassName(NAVIGATION_PACKAGE, "SheetConfigBinding")

internal const val HOME_NAV_PACKAGE: String = "com.thomaskioko.tvmaniac.home.nav"
internal val TabDestination: ClassName = ClassName(HOME_NAV_PACKAGE, "TabDestination")
internal val TabChild: ClassName = ClassName(HOME_NAV_PACKAGE, "TabChild")
