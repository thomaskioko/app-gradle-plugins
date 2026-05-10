package com.thomaskioko.tvmaniac.discover.presenter.di

import com.arkivanov.decompose.ComponentContext
import com.thomaskioko.tvmaniac.core.base.ActivityScope
import com.thomaskioko.tvmaniac.discover.presenter.DiscoverPresenter
import com.thomaskioko.tvmaniac.home.nav.roots.DiscoverRoot
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides

@GraphExtension(DiscoverRoot::class)
public interface DiscoverTabGraph {
    public val discoverPresenter: DiscoverPresenter

    @ContributesTo(ActivityScope::class)
    @GraphExtension.Factory
    public interface Factory {
        public fun createDiscoverTabGraph(@Provides componentContext: ComponentContext): DiscoverTabGraph
    }
}
