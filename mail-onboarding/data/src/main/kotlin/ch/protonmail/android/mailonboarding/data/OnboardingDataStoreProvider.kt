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

package ch.protonmail.android.mailonboarding.data

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import javax.inject.Inject

class OnboardingDataStoreProvider @Inject constructor(
    context: Context
) {

    private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "onboardingPrefDataStore",
        produceMigrations = { _ ->
            listOf(OnboardingV6DataStoreMigration())
        }
    )
    val onboardingDataStore = context.onboardingDataStore

    internal class OnboardingV6DataStoreMigration : DataMigration<Preferences> {

        private val v6Preference = booleanPreferencesKey(V6_SHOW_ONBOARDING_KEY)

        override suspend fun shouldMigrate(currentData: Preferences) = currentData.contains(v6Preference)

        override suspend fun migrate(currentData: Preferences): Preferences {
            val mutablePrefs = currentData.toMutablePreferences()

            val hasV6PreferenceKey = currentData.contains(v6Preference)

            if (hasV6PreferenceKey) {
                mutablePrefs.remove(v6Preference)
            }

            return mutablePrefs.toPreferences()
        }

        override suspend fun cleanUp() = Unit
    }

    internal companion object {

        const val V6_SHOW_ONBOARDING_KEY = "shouldDisplayOnboardingPrefKey"
        const val V7_SHOW_ONBOARDING_KEY = "ShouldDisplayV7BetaOnboardingPrefKey"
        const val PENDING_UPSELL_KEY = "PendingOnboardingUpsellPrefKey"
    }
}
