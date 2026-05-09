package com.thomaskioko.tvmaniac.presenter.showdetails.di

import com.thomaskioko.tvmaniac.core.base.ActivityScope
import com.thomaskioko.tvmaniac.navigation.NavDestination
import com.thomaskioko.tvmaniac.navigation.NavRouteBinding
import com.thomaskioko.tvmaniac.navigation.ScreenDestination
import com.thomaskioko.tvmaniac.showdetails.nav.ShowDetailsRoute
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@ContributesTo(ActivityScope::class)
public interface ShowDetailsNavDestinationBinding {
    public companion object {
        @Provides
        @IntoSet
        public fun provideShowDetailsNavDestination(graphFactory: ShowDetailsScreenGraph.Factory): NavDestination<*> = NavDestination.Screen(
            routeClass = ShowDetailsRoute::class,
            ) { showDetailsRoute, componentContext ->
                val graph = graphFactory.createShowDetailsGraph(componentContext)
                ScreenDestination(graph.showDetailsFactory.create(showDetailsRoute.param))
            }

            @Provides
            @IntoSet
            public fun provideShowDetailsRouteBinding(): NavRouteBinding<*> = NavRouteBinding(ShowDetailsRoute::class, ShowDetailsRoute.serializer())
        }
    }
