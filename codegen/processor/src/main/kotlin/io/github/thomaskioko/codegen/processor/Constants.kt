package io.github.thomaskioko.codegen.processor

/**
 * Fully qualified names and short labels for the annotations and Metro types the processor reads.
 *
 * Centralising these in one object means a typo in a fully qualified name produces one compile
 * error here rather than scattered run time mismatches in the resolver. The same constants back
 * the test stubs in `processor-test/`, so the test compilation sees the same names as production.
 *
 * Renaming or repackaging any of the input annotations means updating this file. The processor
 * resolves symbols by these strings, not by class references, because KSP gives the processor
 * access to symbols, not to the runtime classes themselves.
 */
internal object Constants {
    const val ANNOTATIONS_PACKAGE: String = "io.github.thomaskioko.codegen.annotations"
    const val NAV_DESTINATION: String = "NavDestination"
    const val DESTINATION_KIND: String = "DestinationKind"
    const val SCREEN_UI: String = "ScreenUi"
    const val SHEET_UI: String = "SheetUi"
    const val TAB_UI: String = "TabUi"
    const val CHILD_PRESENTER: String = "ChildPresenter"
    const val APP_ROOT: String = "AppRoot"
    const val APP_ROOT_UI: String = "AppRootUi"

    const val NAV_DESTINATION_FQN: String = "$ANNOTATIONS_PACKAGE.$NAV_DESTINATION"
    const val DESTINATION_KIND_FQN: String = "$ANNOTATIONS_PACKAGE.$DESTINATION_KIND"
    const val SCREEN_UI_FQN: String = "$ANNOTATIONS_PACKAGE.$SCREEN_UI"
    const val SHEET_UI_FQN: String = "$ANNOTATIONS_PACKAGE.$SHEET_UI"
    const val TAB_UI_FQN: String = "$ANNOTATIONS_PACKAGE.$TAB_UI"
    const val CHILD_PRESENTER_FQN: String = "$ANNOTATIONS_PACKAGE.$CHILD_PRESENTER"
    const val APP_ROOT_FQN: String = "$ANNOTATIONS_PACKAGE.$APP_ROOT"
    const val APP_ROOT_UI_FQN: String = "$ANNOTATIONS_PACKAGE.$APP_ROOT_UI"

    const val METRO_PACKAGE: String = "dev.zacsweers.metro"
    const val ASSISTED_FACTORY_FQN: String = "$METRO_PACKAGE.AssistedFactory"
    const val ASSISTED_FQN: String = "$METRO_PACKAGE.Assisted"
    const val ASSISTED_INJECT_FQN: String = "$METRO_PACKAGE.AssistedInject"
}
