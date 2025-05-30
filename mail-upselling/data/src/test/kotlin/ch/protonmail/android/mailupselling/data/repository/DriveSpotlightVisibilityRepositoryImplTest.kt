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

package ch.protonmail.android.mailupselling.data.repository

import java.io.IOException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailupselling.data.UpsellingDataStoreProvider
import ch.protonmail.android.mailupselling.domain.model.DriveSpotlightLastSeenPreference
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class DriveSpotlightVisibilityRepositoryImplTest {

    private val preferences = mockk<Preferences>()
    private val mockDataStore = spyk<DataStore<Preferences>>()
    private val dataStoreProvider = mockk<UpsellingDataStoreProvider> {
        every { this@mockk.upsellingDataStore } returns mockDataStore
    }
    private val sut = DriveSpotlightVisibilityRepositoryImpl(dataStoreProvider)

    @Test
    fun `returns null when no preference is stored locally`() = runTest {
        // Given
        coEvery { preferences.get<Boolean>(any()) } returns null
        every { mockDataStore.data } returns flowOf(preferences)

        // When
        sut.observe().test {
            // Then
            assertEquals(DefaultValue.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns locally stored preference from data store when available`() = runTest {
        // Given
        coEvery { preferences[longPreferencesKey("driveSpotlightLastSeenPreference")] } returns
            AnotherValue.seenTimestamp
        every { mockDataStore.data } returns flowOf(preferences)

        // When
        sut.observe().test {
            // Then
            assertEquals(AnotherValue.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return error when an exception is thrown while observing preference`() = runTest {
        // Given
        every { mockDataStore.data } returns flow { throw IOException() }

        // When
        sut.observe().test {
            // Then
            assertEquals(PreferencesError.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return success when preference is updated`() = runTest {
        // Given
        val expectedResult = Unit.right()

        // When
        val result = sut.update(AnotherValue.seenTimestamp!!)

        // Then
        coVerify { mockDataStore.updateData(any()) }
        assertEquals(expectedResult, result)
    }

    @Test
    fun `should return failure when an exception is thrown while saving preference`() = runTest {
        // Given
        val expectedResult = PreferencesError.left()
        coEvery { mockDataStore.updateData(any()) } throws IOException()

        // When
        val result = sut.update(AnotherValue.seenTimestamp!!)

        // Then
        assertEquals(expectedResult, result)
    }

    private companion object {

        val DefaultValue = DriveSpotlightLastSeenPreference(null)
        val AnotherValue = DriveSpotlightLastSeenPreference(123L)
    }
}
