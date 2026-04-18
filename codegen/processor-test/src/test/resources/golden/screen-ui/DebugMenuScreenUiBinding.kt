package com.thomaskioko.tvmaniac.debug.ui.di

import com.thomaskioko.tvmaniac.core.base.ActivityScope
import com.thomaskioko.tvmaniac.debug.presenter.DebugPresenter
import com.thomaskioko.tvmaniac.debug.ui.DebugMenuScreen
import com.thomaskioko.tvmaniac.navigation.ScreenDestination
import com.thomaskioko.tvmaniac.navigation.ui.ScreenContent
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@BindingContainer
@ContributesTo(ActivityScope::class)
public object DebugMenuScreenUiBinding {
    @Provides
    @IntoSet
    public fun provideDebugMenuScreenContent(): ScreenContent = ScreenContent(
        matches = { (it as? ScreenDestination<*>)?.presenter is DebugPresenter },
        content = { child, modifier ->
            DebugMenuScreen(
                presenter = (child as ScreenDestination<*>).presenter as DebugPresenter,
                modifier = modifier,
            )
        },
    )
}
