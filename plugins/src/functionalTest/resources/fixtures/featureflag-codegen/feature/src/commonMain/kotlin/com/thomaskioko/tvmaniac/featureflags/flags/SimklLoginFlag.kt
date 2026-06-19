package com.thomaskioko.tvmaniac.featureflags.flags

import io.github.thomaskioko.codegen.annotations.FeatureFlag

@FeatureFlag(
    key = "simkl_login_enabled",
    title = "Simkl Login",
    description = "Show the Simkl login entry point on the settings screen.",
    defaultValue = false,
    dateAdded = "2026-05-17",
)
public object SimklLoginFlag
