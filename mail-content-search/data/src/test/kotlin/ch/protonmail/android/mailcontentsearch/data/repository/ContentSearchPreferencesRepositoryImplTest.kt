/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import ch.protonmail.android.mailcontentsearch.data.local.ContentSearchDataStoreProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ContentSearchPreferencesRepositoryImplTest {

    private val dataStore = mockk<DataStore<Preferences>>()
    private val dataStoreProvider = mockk<ContentSearchDataStoreProvider> {
        every { allowMobileDataDataStore } returns dataStore
    }
    private val repository = ContentSearchPreferencesRepositoryImpl(dataStoreProvider)

    @Test
    fun `returns the stored allow mobile data value when present`() = runTest {
        // Given
        val preferences = mockk<Preferences> {
            every { get(any<Preferences.Key<Boolean>>()) } returns false
        }
        every { dataStore.data } returns flowOf(preferences)

        // When
        val result = repository.getAllowMobileData()

        // Then
        assertFalse(result.getOrNull()!!)
    }

    @Test
    fun `defaults to allowing mobile data when no value is stored`() = runTest {
        // Given
        val preferences = mockk<Preferences> {
            every { get(any<Preferences.Key<Boolean>>()) } returns null
        }
        every { dataStore.data } returns flowOf(preferences)

        // When
        val result = repository.getAllowMobileData()

        // Then
        assertTrue(result.getOrNull()!!)
    }

    @Test
    fun `persists the allow mobile data value through the data store`() = runTest {
        // Given
        coEvery { dataStore.updateData(any()) } returns mockk()

        // When
        repository.setAllowMobileData(true)

        // Then
        coVerify { dataStore.updateData(any()) }
    }
}
