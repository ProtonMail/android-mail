/*
 * Copyright (c) 2023 Proton AG
 * This file is part of Proton AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.initializer

import android.content.Context
import androidx.startup.Initializer
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.initializer.featureflag.RefreshNPSFeedbackFeatureFlags
import ch.protonmail.android.initializer.featureflag.RefreshRatingBoosterFeatureFlags
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import me.proton.core.featureflag.data.FeatureFlagRefreshStarter

class FeatureFlagInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            FeatureFlagInitializerEntryPoint::class.java
        )
        entryPoint.featureFlagRefreshStarter().start(BuildConfig.DEBUG)
        entryPoint.refreshRatingBoosterFeatureFlags().invoke()
        entryPoint.refreshNPSFeedbackFeatureFlags().invoke()
    }

    override fun dependencies(): List<Class<out Initializer<*>?>> = listOf(
        WorkManagerInitializer::class.java
    )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FeatureFlagInitializerEntryPoint {
        fun featureFlagRefreshStarter(): FeatureFlagRefreshStarter

        fun refreshRatingBoosterFeatureFlags(): RefreshRatingBoosterFeatureFlags

        fun refreshNPSFeedbackFeatureFlags(): RefreshNPSFeedbackFeatureFlags
    }
}
