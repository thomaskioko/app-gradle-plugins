package io.github.thomaskioko.codegen.processor

internal object Constants {
    const val ANNOTATIONS_PACKAGE: String = "io.github.thomaskioko.codegen.annotations"
    const val NAV_SCREEN: String = "NavScreen"
    const val TAB_SCREEN: String = "TabScreen"
    const val NAV_SHEET: String = "NavSheet"

    const val NAV_SCREEN_FQN: String = "$ANNOTATIONS_PACKAGE.$NAV_SCREEN"
    const val TAB_SCREEN_FQN: String = "$ANNOTATIONS_PACKAGE.$TAB_SCREEN"
    const val NAV_SHEET_FQN: String = "$ANNOTATIONS_PACKAGE.$NAV_SHEET"

    const val METRO_PACKAGE: String = "dev.zacsweers.metro"
    const val ASSISTED_FACTORY_FQN: String = "$METRO_PACKAGE.AssistedFactory"
    const val ASSISTED_FQN: String = "$METRO_PACKAGE.Assisted"
}
