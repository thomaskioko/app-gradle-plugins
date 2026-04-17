package com.thomaskioko.tvmaniac.discover.presenter.di

import com.arkivanov.decompose.ComponentContext
import com.thomaskioko.tvmaniac.discover.presenter.DiscoverShowsPresenter
import com.thomaskioko.tvmaniac.home.nav.di.model.HomeConfig
import com.thomaskioko.tvmaniac.home.nav.scope.HomeScreenScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides

@GraphExtension(HomeConfig.Discover::class)
public interface DiscoverShowsTabGraph {
    public val discoverShowsPresenter: DiscoverShowsPresenter

    @ContributesTo(HomeScreenScope::class)
    @GraphExtension.Factory
    public interface Factory {
        public fun createDiscoverShowsTabGraph(@Provides componentContext: ComponentContext): DiscoverShowsTabGraph
    }
}
