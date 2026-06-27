package com.thomaskioko.tvmaniac.presentation.seasons.di

import com.arkivanov.decompose.ComponentContext
import com.thomaskioko.tvmaniac.presentation.seasons.SeasonsPresenter
import com.thomaskioko.tvmaniac.progress.nav.ProgressRoot
import com.thomaskioko.tvmaniac.progress.nav.scope.ProgressChildScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides

@GraphExtension(ProgressChildScope::class)
public interface SeasonsChildGraph {
    public val seasonsFactory: SeasonsPresenter.Factory

    @ContributesTo(ProgressRoot::class)
    @GraphExtension.Factory
    public interface Factory {
        public fun createSeasonsGraph(@Provides componentContext: ComponentContext): SeasonsChildGraph
    }
}
