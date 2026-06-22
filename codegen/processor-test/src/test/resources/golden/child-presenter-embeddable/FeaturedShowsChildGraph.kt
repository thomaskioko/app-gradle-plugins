package com.thomaskioko.tvmaniac.presentation.featured.di

import com.arkivanov.decompose.ComponentContext
import com.thomaskioko.tvmaniac.core.base.ActivityScope
import com.thomaskioko.tvmaniac.featured.nav.scope.FeaturedShowsComponentScope
import com.thomaskioko.tvmaniac.presentation.featured.FeaturedShowsPresenter
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides

@GraphExtension(FeaturedShowsComponentScope::class)
public interface FeaturedShowsChildGraph {
    public val featuredShowsPresenter: FeaturedShowsPresenter

    @ContributesTo(ActivityScope::class)
    @GraphExtension.Factory
    public interface Factory {
        public fun createFeaturedShowsGraph(@Provides componentContext: ComponentContext): FeaturedShowsChildGraph
    }
}
