package com.thomaskioko.tvmaniac.featureflags.flags

import com.thomaskioko.tvmaniac.featureflags.FeatureFlag
import com.thomaskioko.tvmaniac.featureflags.FeatureFlagFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.Boolean
import kotlinx.datetime.LocalDate

@ContributesTo(AppScope::class)
public interface ContinueWatchingNitroFlagBinding {
  @Provides
  @SingleIn(AppScope::class)
  @ContinueWatchingNitroFlagQualifier
  public fun provideContinueWatchingNitroFlag(factory: FeatureFlagFactory): FeatureFlag<Boolean> = factory.boolean(
      key = "enable_continue_watching_nitro",
      title = "Progress Endpoint",
      description = "Use Trakt's internal /sync/progress/up_next_nitro call instead of the documented multi-step progress fetch.",
      defaultValue = false,
      dateAdded = LocalDate(2026, 5, 20),
  )

  @Provides
  @IntoSet
  public fun bindContinueWatchingNitroFlag(@ContinueWatchingNitroFlagQualifier flag: FeatureFlag<Boolean>): FeatureFlag<Boolean> = flag
}
