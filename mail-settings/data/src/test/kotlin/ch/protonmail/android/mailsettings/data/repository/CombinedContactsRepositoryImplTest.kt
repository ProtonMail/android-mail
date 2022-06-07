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

package ch.protonmail.android.mailsettings.data.repository

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import app.cash.turbine.test
import ch.protonmail.android.mailsettings.data.MailSettingsDataStoreProvider
import ch.protonmail.android.mailsettings.domain.model.CombinedContactsPreference
import ch.protonmail.android.mailsettings.domain.repository.CombinedContactsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CombinedContactsRepositoryImplTest {

    private val preferences = mockk<Preferences>()
    private val dataStoreProvider = mockk<MailSettingsDataStoreProvider> {
        every { this@mockk.combinedContactsDataStore } returns mockk dataStore@{
            every { this@dataStore.data } returns flowOf(preferences)
        }
    }

    private lateinit var combinedContactsRepository: CombinedContactsRepository

    @Before
    fun setUp() {
        combinedContactsRepository = CombinedContactsRepositoryImpl(dataStoreProvider)
    }

    @Test
    fun `returns false when no preference is stored locally`() = runTest {
        // Given
        coEvery { preferences.get<Boolean>(any()) } returns null
        // When
        combinedContactsRepository.observe().test {
            // Then
            assertEquals(CombinedContactsPreference(false), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns locally stored preference from data store when available`() = runTest {
        // Given
        coEvery { preferences[booleanPreferencesKey("hasCombinedContactsPrefKey")] } returns true
        // When
        combinedContactsRepository.observe().test {
            // Then
            assertEquals(CombinedContactsPreference(true), awaitItem())
            awaitComplete()
        }
    }
}
