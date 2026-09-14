package com.thomaskioko.tvmaniac.i18n

import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.ResourceContainer
import dev.icerock.moko.resources.ResourcePlatformDetails
import dev.icerock.moko.resources.StringResource
import kotlin.collections.List

public expect object MR {
  public object strings : ResourceContainer<StringResource> {
    override val __platformDetails: ResourcePlatformDetails

    override fun values(): List<StringResource>
  }

  public object plurals : ResourceContainer<PluralsResource> {
    override val __platformDetails: ResourcePlatformDetails

    override fun values(): List<PluralsResource>
  }
}
