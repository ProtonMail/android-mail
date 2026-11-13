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
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailspotlight.data.FeatureSpotlightDataStoreProvider
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Reads the What's New version the app recorded as seen before the SDK owned the announcements. It is only an input to
 * the SDK migration: nothing writes it any more.
 */
class FeatureSpotlightLocalDataSourceImpl @Inject constructor(
    private val dataStoreProvider: FeatureSpotlightDataStoreProvider
) : FeatureSpotlightLocalDataSource {

    private val lastSeenSpotlightVersionKey = intPreferencesKey(FeatureSpotlightDataStoreProvider.FEATURE_SPOTLIGHT_KEY)

    override suspend fun isContentSearchWhatsNewSeen(): Either<PreferencesError, Boolean> =
        dataStoreProvider.featureSpotlightDataStore.safeData.first().map { prefs ->
            val lastSeenSpotlightVersion = prefs[lastSeenSpotlightVersionKey] ?: DEFAULT_VALUE
            lastSeenSpotlightVersion >= FeatureSpotlightVersions.CONTENT_SEARCH
        }

    private companion object {

        const val DEFAULT_VALUE = 0
    }
}
