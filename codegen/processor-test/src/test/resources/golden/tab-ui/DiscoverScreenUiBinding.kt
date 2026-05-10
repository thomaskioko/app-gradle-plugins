package com.thomaskioko.tvmaniac.discover.ui.di

import com.thomaskioko.tvmaniac.core.base.ActivityScope
import com.thomaskioko.tvmaniac.discover.presenter.DiscoverShowsPresenter
import com.thomaskioko.tvmaniac.discover.ui.DiscoverScreen
import com.thomaskioko.tvmaniac.home.nav.TabChild
import com.thomaskioko.tvmaniac.navigation.ui.ScreenContent
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@BindingContainer
@ContributesTo(ActivityScope::class)
public object DiscoverScreenUiBinding {
    @Provides
    @IntoSet
    public fun provideDiscoverScreenContent(): ScreenContent = ScreenContent(
        matches = { (it as? TabChild<*>)?.presenter is DiscoverShowsPresenter },
        content = { child, modifier ->
            DiscoverScreen(
                presenter = (child as TabChild<*>).presenter as DiscoverShowsPresenter,
                modifier = modifier,
            )
        },
    )
}
