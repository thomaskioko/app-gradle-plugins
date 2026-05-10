package com.thomaskioko.tvmaniac.app.ui.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thomaskioko.tvmaniac.app.ui.RootScreen
import com.thomaskioko.tvmaniac.navigation.ui.ScreenContent
import com.thomaskioko.tvmaniac.navigation.ui.SheetContent
import com.thomaskioko.tvmaniac.presenter.root.RootPresenter
import kotlin.collections.Set

public interface AppRootProvider {
    public val rootPresenter: RootPresenter

    public val screenContents: Set<ScreenContent>

    public val sheetContents: Set<SheetContent>
}

@Composable
public fun AppRootProvider.AppRootContent(modifier: Modifier = Modifier) {
    RootScreen(
        rootPresenter = rootPresenter,
        screenContents = screenContents,
        sheetContents = sheetContents,
        modifier = modifier,
    )
}
