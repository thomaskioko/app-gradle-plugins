package com.thomaskioko.tvmaniac.i18n

import dev.icerock.moko.resources.StringResource
import kotlin.Lazy

public sealed class StringResourceKey(
  private val resourceProvider: Lazy<StringResource>,
) {
  public val resourceId: StringResource
    get() = resourceProvider.value

  public data object ButtonErrorRetry : StringResourceKey(lazy { MR.strings.button_error_retry })

  public data object AppName : StringResourceKey(lazy { MR.strings.app_name })

  public data object LabelDiscoverTrendingToday : StringResourceKey(lazy { MR.strings.label_discover_trending_today })
}
