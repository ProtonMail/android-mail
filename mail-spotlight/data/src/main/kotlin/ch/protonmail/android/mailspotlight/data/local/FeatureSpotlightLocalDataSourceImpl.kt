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
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class FeatureSpotlightLocalDataSourceImpl @Inject constructor(
    private val dataStoreProvider: FeatureSpotlightDataStoreProvider
) : FeatureSpotlightLocalDataSource {

    // The pre-7.11.3 flag, one per device and shared by all accounts. Read but never written here anymore.
    private val legacyDevicePrefKey = intPreferencesKey(FeatureSpotlightDataStoreProvider.FEATURE_SPOTLIGHT_KEY)

    override fun observe(userId: UserId): Flow<Either<PreferencesError, FeatureSpotlightDisplay>> =
        dataStoreProvider.featureSpotlightDataStore.safeData.map { prefsEither ->
            prefsEither.map { prefs ->
                val legacyDeviceValue = prefs[legacyDevicePrefKey] ?: DEFAULT_VALUE
                val perUserValue = prefs[prefKeyFor(userId)] ?: DEFAULT_VALUE
                // Users who saw the spotlight before per-account tracking existed (pre-7.11.3) keep the
                // old behaviour: the legacy device-wide flag suppresses it for every account.
                val show = if (legacyDeviceValue >= CURRENT_SPOTLIGHT_VERSION) {
                    false
                } else {
                    perUserValue < CURRENT_SPOTLIGHT_VERSION
                }
                Timber.d(
                    "Spotlight userId=${userId.id} show=$show legacyDeviceValue=$legacyDeviceValue " +
                        "perUserValue=$perUserValue currentVersion=$CURRENT_SPOTLIGHT_VERSION"
                )
                FeatureSpotlightDisplay(show)
            }
        }

    override suspend fun save(userId: UserId): Either<PreferencesError, Unit> =
        dataStoreProvider.featureSpotlightDataStore.safeEdit { mutablePreferences ->
            // Persist per account only
            mutablePreferences[prefKeyFor(userId)] = CURRENT_SPOTLIGHT_VERSION
        }.map { }

    // Scoped per account, so the spotlight replays once for each account.
    private fun prefKeyFor(userId: UserId) = intPreferencesKey(
        "${userId.id}-${FeatureSpotlightDataStoreProvider.FEATURE_SPOTLIGHT_KEY}"
    )

    private companion object {

        const val DEFAULT_VALUE = 0

        // Update this when releasing a new Feature Spotlight
        const val CURRENT_SPOTLIGHT_VERSION = FeatureSpotlightVersions.CATEGORY_VIEW

    }
}
