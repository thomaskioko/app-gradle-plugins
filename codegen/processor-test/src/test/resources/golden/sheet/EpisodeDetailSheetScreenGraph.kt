package com.thomaskioko.tvmaniac.presentation.episodedetail.di

import com.arkivanov.decompose.ComponentContext
import com.thomaskioko.root.model.EpisodeSheetConfig
import com.thomaskioko.tvmaniac.core.base.ActivityScope
import com.thomaskioko.tvmaniac.presentation.episodedetail.EpisodeDetailSheetPresenter
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides

@GraphExtension(EpisodeSheetConfig::class)
public interface EpisodeDetailSheetScreenGraph {
    public val episodeDetailSheetFactory: EpisodeDetailSheetPresenter.Factory

    @ContributesTo(ActivityScope::class)
    @GraphExtension.Factory
    public interface Factory {
        public fun createEpisodeDetailSheetGraph(@Provides componentContext: ComponentContext): EpisodeDetailSheetScreenGraph
    }
}
