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

package ch.protonmail.android.mailonboarding.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import arrow.core.Either
import ch.protonmail.android.mailcommon.data.mapper.safeData
import ch.protonmail.android.mailcommon.data.mapper.safeEdit
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailonboarding.data.OnboardingDataStoreProvider
import ch.protonmail.android.mailonboarding.domain.model.OnboardingPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OnboardingLocalDataSourceImpl @Inject constructor(
    private val dataStoreProvider: OnboardingDataStoreProvider
) : OnboardingLocalDataSource {

    private val shouldDisplayOnboardingPrefKey = booleanPreferencesKey("shouldDisplayOnboardingPrefKey")

    override fun observe(): Flow<Either<PreferencesError, OnboardingPreference>> =
        dataStoreProvider.onboardingDataStore.safeData.map { prefsEither ->
            prefsEither.map { prefs ->
                val shouldDisplayOnboarding = prefs[shouldDisplayOnboardingPrefKey] ?: DEFAULT_VALUE
                OnboardingPreference(shouldDisplayOnboarding)
            }
        }

    override suspend fun save(onboardingPreference: OnboardingPreference): Either<PreferencesError, Unit> =
        dataStoreProvider.onboardingDataStore.safeEdit { mutablePreferences ->
            mutablePreferences[shouldDisplayOnboardingPrefKey] = onboardingPreference.display
        }.void()
}

private const val DEFAULT_VALUE = true
