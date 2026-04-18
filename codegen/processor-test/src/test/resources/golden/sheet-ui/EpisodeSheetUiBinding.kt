package com.thomaskioko.tvmaniac.episodedetail.ui.di

import com.thomaskioko.tvmaniac.core.base.ActivityScope
import com.thomaskioko.tvmaniac.episodedetail.ui.EpisodeSheet
import com.thomaskioko.tvmaniac.navigation.SheetDestination
import com.thomaskioko.tvmaniac.navigation.ui.SheetContent
import com.thomaskioko.tvmaniac.presentation.episodedetail.EpisodeSheetPresenter
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@BindingContainer
@ContributesTo(ActivityScope::class)
public object EpisodeSheetUiBinding {
    @Provides
    @IntoSet
    public fun provideEpisodeSheetContent(): SheetContent = SheetContent(
        matches = { (it as? SheetDestination<*>)?.presenter is EpisodeSheetPresenter },
        content = { child ->
            EpisodeSheet(
                presenter = (child as SheetDestination<*>).presenter as EpisodeSheetPresenter,
            )
        },
    )
}
