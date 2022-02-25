/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailsettings.data.repository

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import app.cash.turbine.test
import ch.protonmail.android.mailsettings.data.DataStoreProvider
import ch.protonmail.android.mailsettings.domain.model.AlternativeRoutingPreference
import ch.protonmail.android.mailsettings.domain.repository.AlternativeRoutingRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AlternativeRoutingRepositoryImplTest {

    private val preferences = mockk<Preferences>()
    private val dataStoreProvider = mockk<DataStoreProvider> {
        every { this@mockk.alternativeRoutingDataStore } returns mockk dataStore@{
            every { this@dataStore.data } returns flowOf(preferences)
        }
    }

    private lateinit var alternativeRoutingRepository: AlternativeRoutingRepository

    @Before
    fun setUp() {
        alternativeRoutingRepository = AlternativeRoutingRepositoryImpl(dataStoreProvider)
    }

    @Test
    fun returnsTrueWhenNoPreferenceIsStoredLocally() = runTest {
        // Given
        coEvery { preferences.get<Boolean>(any()) } returns null
        // When
        alternativeRoutingRepository.observe().test {
            // Then
            assertEquals(AlternativeRoutingPreference(true), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun returnsLocallyStoredPreferenceFromDataStoreWhenAvailable() = runTest {
        // Given
        coEvery { preferences[booleanPreferencesKey("hasAlternativeRoutingPrefKey")] } returns false
        // When
        alternativeRoutingRepository.observe().test {
            // Then
            assertEquals(AlternativeRoutingPreference(false), awaitItem())
            awaitComplete()
        }
    }
}
