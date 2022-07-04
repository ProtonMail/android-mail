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

import java.io.IOException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import app.cash.turbine.test
import ch.protonmail.android.mailsettings.data.MailSettingsDataStoreProvider
import ch.protonmail.android.mailsettings.domain.model.CombinedContactsPreference
import ch.protonmail.android.mailsettings.domain.repository.CombinedContactsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import kotlin.test.Test

class CombinedContactsRepositoryImplTest {

    private val preferences = mockk<Preferences>()
    private val combinedContactsDataStoreSpy = spyk<DataStore<Preferences>> {
        every { this@spyk.data } returns flowOf(preferences)
    }
    private val dataStoreProvider = mockk<MailSettingsDataStoreProvider> {
        every { this@mockk.combinedContactsDataStore } returns combinedContactsDataStoreSpy
    }

    private val combinedContactsRepository: CombinedContactsRepository =
        CombinedContactsRepositoryImpl(dataStoreProvider)

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

    @Test
    fun `should return success when preference is saved`() = runTest {
        // Given
        val combinedContactsPreference = CombinedContactsPreference(isEnabled = true)

        // When
        val result = combinedContactsRepository.save(combinedContactsPreference)

        // Then
        coVerify { combinedContactsDataStoreSpy.updateData(any()) }
        assertEquals(Result.success(Unit), result)
    }

    @Test
    fun `should return failure when an exception is thrown while saving preference`() = runTest {
        // Given
        val combinedContactsPreference = CombinedContactsPreference(isEnabled = true)
        val ioException = IOException()
        coEvery { combinedContactsDataStoreSpy.updateData(any()) } throws ioException

        // When
        val result = combinedContactsRepository.save(combinedContactsPreference)

        // Then
        assertEquals(Result.failure<Unit>(ioException), result)
    }
}
