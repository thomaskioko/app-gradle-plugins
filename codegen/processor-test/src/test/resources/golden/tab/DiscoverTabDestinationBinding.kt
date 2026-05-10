package com.thomaskioko.tvmaniac.discover.presenter.di

import com.thomaskioko.tvmaniac.core.base.ActivityScope
import com.thomaskioko.tvmaniac.home.nav.TabChild
import com.thomaskioko.tvmaniac.home.nav.roots.DiscoverRoot
import com.thomaskioko.tvmaniac.navigation.NavDestination
import com.thomaskioko.tvmaniac.navigation.NavRoot
import com.thomaskioko.tvmaniac.navigation.NavRootBinding
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@ContributesTo(ActivityScope::class)
public interface DiscoverTabDestinationBinding {
    public companion object {
        @Provides
        @IntoSet
        public fun provideDiscoverNavDestination(graphFactory: DiscoverTabGraph.Factory): NavDestination<*> = NavDestination.TabRoot(
            routeClass = DiscoverRoot::class,
            ) { _, componentContext ->
                TabChild(graphFactory.createDiscoverTabGraph(componentContext).discoverPresenter)
            }

            @Provides
            @IntoSet
            public fun provideDiscoverNavRoot(): NavRoot = DiscoverRoot

            @Provides
            @IntoSet
            public fun provideDiscoverRootBinding(): NavRootBinding<*> = NavRootBinding(DiscoverRoot::class, DiscoverRoot.serializer())
        }
    }
