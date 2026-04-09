package com.thomaskioko.tvmaniac.i18n

import dev.icerock.moko.resources.PluralsResource
import kotlin.Lazy

public sealed class PluralsResourceKey(
  private val resourceProvider: Lazy<PluralsResource>,
) {
  public val resourceId: PluralsResource
    get() = resourceProvider.value

  public data object EpisodeCount : PluralsResourceKey(lazy { MR.plurals.episode_count })

  public data object SeasonCount : PluralsResourceKey(lazy { MR.plurals.season_count })
}
