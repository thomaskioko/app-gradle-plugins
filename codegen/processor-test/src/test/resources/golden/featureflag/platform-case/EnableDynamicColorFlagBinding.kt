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
public interface EnableDynamicColorFlagBinding {
  @Provides
  @SingleIn(AppScope::class)
  @EnableDynamicColorFlagQualifier
  public fun provideEnableDynamicColorFlag(factory: FeatureFlagFactory): FeatureFlag<Boolean> = factory.boolean(
      key = "enable_dynamic_color",
      title = "Dynamic Color",
      description = "Apply the Android Material You dynamic color scheme.",
      defaultValue = false,
      dateAdded = LocalDate(2026, 6, 18),
  )

  @Provides
  @IntoSet
  public fun bindEnableDynamicColorFlag(@EnableDynamicColorFlagQualifier flag: FeatureFlag<Boolean>): FeatureFlag<Boolean> = flag
}
