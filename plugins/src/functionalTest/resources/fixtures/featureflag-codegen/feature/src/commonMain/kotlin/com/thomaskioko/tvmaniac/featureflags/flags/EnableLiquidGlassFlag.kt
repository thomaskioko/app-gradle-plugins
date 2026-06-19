package com.thomaskioko.tvmaniac.featureflags.flags

import io.github.thomaskioko.codegen.annotations.FeatureFlag
import io.github.thomaskioko.codegen.annotations.Platform

@FeatureFlag(
    key = "enable_liquid_glass",
    title = "Liquid Glass",
    description = "Render the iOS debug screen with the Liquid Glass material.",
    defaultValue = false,
    dateAdded = "2026-06-18",
    platform = Platform.IOS,
)
public object EnableLiquidGlassFlag
