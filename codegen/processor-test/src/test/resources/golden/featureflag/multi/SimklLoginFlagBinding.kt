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
public interface SimklLoginFlagBinding {
  @Provides
  @SingleIn(AppScope::class)
  @SimklLoginFlagQualifier
  public fun provideSimklLoginFlag(factory: FeatureFlagFactory): FeatureFlag<Boolean> = factory.boolean(
      key = "simkl_login_enabled",
      title = "Simkl Login",
      description = "Show the Simkl login entry point on the settings screen.",
      defaultValue = false,
      dateAdded = LocalDate(2_026, 5, 17),
  )

  @Provides
  @IntoSet
  public fun bindSimklLoginFlag(@SimklLoginFlagQualifier flag: FeatureFlag<Boolean>): FeatureFlag<Boolean> = flag
}
