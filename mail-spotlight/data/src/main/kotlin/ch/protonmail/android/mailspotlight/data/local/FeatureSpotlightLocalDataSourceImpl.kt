/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
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

package ch.protonmail.android.mailspotlight.data.local

import androidx.datastore.preferences.core.intPreferencesKey
import arrow.core.Either
import ch.protonmail.android.mailcommon.data.mapper.safeData
import ch.protonmail.android.mailcommon.data.mapper.safeEdit
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailspotlight.data.FeatureSpotlightDataStoreProvider
import ch.protonmail.android.mailspotlight.domain.model.FeatureSpotlightDisplay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FeatureSpotlightLocalDataSourceImpl @Inject constructor(
    private val dataStoreProvider: FeatureSpotlightDataStoreProvider
) : FeatureSpotlightLocalDataSource {

    private val shouldDisplayFeatureSpotlightPrefKey = intPreferencesKey(
        FeatureSpotlightDataStoreProvider.FEATURE_SPOTLIGHT_KEY
    )

    override fun observe(): Flow<Either<PreferencesError, FeatureSpotlightDisplay>> =
        dataStoreProvider.featureSpotlightDataStore.safeData.map { prefsEither ->
            prefsEither.map { prefs ->
                val lastSeenSpotlightVersion = prefs[shouldDisplayFeatureSpotlightPrefKey] ?: DEFAULT_VALUE

                val shouldShow = lastSeenSpotlightVersion < CURRENT_SPOTLIGHT_VERSION
                FeatureSpotlightDisplay(shouldShow)
            }
        }

    override suspend fun save(): Either<PreferencesError, Unit> =
        dataStoreProvider.featureSpotlightDataStore.safeEdit { mutablePreferences ->
            mutablePreferences[shouldDisplayFeatureSpotlightPrefKey] = CURRENT_SPOTLIGHT_VERSION
        }.map { }

    private companion object {

        const val DEFAULT_VALUE = 0

        // Update this when releasing a new Feature Spotlight
        const val CURRENT_SPOTLIGHT_VERSION = FeatureSpotlightVersions.CATEGORY_VIEW
    }
}
