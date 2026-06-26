package com.thomaskioko.tvmaniac.presentation.upnext.di

import com.arkivanov.decompose.ComponentContext
import com.thomaskioko.tvmaniac.presentation.upnext.UpNextPresenter
import com.thomaskioko.tvmaniac.progress.nav.ProgressRoot
import com.thomaskioko.tvmaniac.progress.nav.scope.ProgressChildScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides

@GraphExtension(ProgressChildScope::class)
public interface UpNextChildGraph {
    public val upNextPresenter: UpNextPresenter

    @ContributesTo(ProgressRoot::class)
    @GraphExtension.Factory
    public interface Factory {
        public fun createUpNextGraph(@Provides componentContext: ComponentContext): UpNextChildGraph
    }
}
