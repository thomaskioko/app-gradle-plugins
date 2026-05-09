package com.thomaskioko.tvmaniac.debug.presenter.di

import com.thomaskioko.tvmaniac.core.base.ActivityScope
import com.thomaskioko.tvmaniac.debug.nav.DebugRoute
import com.thomaskioko.tvmaniac.navigation.NavDestination
import com.thomaskioko.tvmaniac.navigation.NavRouteBinding
import com.thomaskioko.tvmaniac.navigation.ScreenDestination
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@ContributesTo(ActivityScope::class)
public interface DebugNavDestinationBinding {
    public companion object {
        @Provides
        @IntoSet
        public fun provideDebugNavDestination(graphFactory: DebugScreenGraph.Factory): NavDestination<*> = NavDestination.Screen(
            routeClass = DebugRoute::class,
            ) { _, componentContext ->
                ScreenDestination(graphFactory.createDebugGraph(componentContext).debugPresenter)
            }

            @Provides
            @IntoSet
            public fun provideDebugRouteBinding(): NavRouteBinding<*> = NavRouteBinding(DebugRoute::class, DebugRoute.serializer())
        }
    }
