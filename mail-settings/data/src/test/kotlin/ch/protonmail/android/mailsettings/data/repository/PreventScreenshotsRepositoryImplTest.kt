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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.data.MailSettingsDataStoreProvider
import ch.protonmail.android.mailsettings.domain.model.PreventScreenshotsPreference
import ch.protonmail.android.mailsettings.domain.repository.PreventScreenshotsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

internal class PreventScreenshotsRepositoryImplTest {

    private val preferences = mockk<Preferences>()
    private val preventScreenshotsDataStoreSpy = spyk<DataStore<Preferences>>()
    private val dataStoreProvider = mockk<MailSettingsDataStoreProvider> {
        every { this@mockk.preventScreenshotsDataStore } returns preventScreenshotsDataStoreSpy
    }

    private val preventScreenshotsRepository: PreventScreenshotsRepository =
        PreventScreenshotsRepositoryImpl(dataStoreProvider)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `returns false when no preference is stored locally`() = runTest {
        // Given
        coEvery { preferences.get<Boolean>(any()) } returns null
        every { preventScreenshotsDataStoreSpy.data } returns flowOf(preferences)

        // When
        preventScreenshotsRepository.observe().test {
            // Then
            assertEquals(PreventScreenshotsPreference(false).right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns locally stored preference from data store when available`() = runTest {
        // Given
        coEvery { preferences[booleanPreferencesKey("preventScreenshotsEnumPrefKey")] } returns true
        every { preventScreenshotsDataStoreSpy.data } returns flowOf(preferences)

        // When
        preventScreenshotsRepository.observe().test {
            // Then
            assertEquals(PreventScreenshotsPreference(true).right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return error when an exception is thrown while observing preference`() = runTest {
        // Given
        every { preventScreenshotsDataStoreSpy.data } returns flow { throw IOException() }

        // When
        preventScreenshotsRepository.observe().test {
            // Then
            assertEquals(PreferencesError.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return success when preference is updated`() = runTest {
        // Given
        val expectedResult = Unit.right()
        val preventScreenshotsPreference = PreventScreenshotsPreference(isEnabled = true)

        // When
        val result = preventScreenshotsRepository.update(preventScreenshotsPreference.isEnabled)

        // Then
        coVerify { preventScreenshotsDataStoreSpy.updateData(any()) }
        assertEquals(expectedResult, result)
    }

    @Test
    fun `should return failure when an exception is thrown while saving preference`() = runTest {
        // Given
        val expectedResult = PreferencesError.left()
        val preventScreenshotsPreference = PreventScreenshotsPreference(isEnabled = true)
        coEvery { preventScreenshotsDataStoreSpy.updateData(any()) } throws IOException()

        // When
        val result = preventScreenshotsRepository.update(preventScreenshotsPreference.isEnabled)

        // Then
        assertEquals(expectedResult, result)
    }
}
