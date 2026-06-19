package com.thomaskioko.tvmaniac.featureflags

import kotlinx.datetime.LocalDate

public interface FeatureFlag<T>

public interface FeatureFlagFactory {
    public fun boolean(
        key: String,
        title: String,
        description: String,
        defaultValue: Boolean,
        dateAdded: LocalDate,
    ): FeatureFlag<Boolean>
}
