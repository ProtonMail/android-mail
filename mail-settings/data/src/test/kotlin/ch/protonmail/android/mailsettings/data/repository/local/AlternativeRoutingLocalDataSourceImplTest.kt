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

package ch.protonmail.android.mailsettings.data.repository.local

import java.io.IOException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.data.MailSettingsDataStoreProvider
import ch.protonmail.android.mailsettings.domain.model.AlternativeRoutingPreference
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class AlternativeRoutingLocalDataSourceImplTest {

    private val preferences = mockk<Preferences>()
    private val alternativeRoutingDataStoreMock = mockk<DataStore<Preferences>>()
    private val dataStoreProvider = mockk<MailSettingsDataStoreProvider> {
        every { this@mockk.alternativeRoutingDataStore } returns alternativeRoutingDataStoreMock
    }

    private val alternativeRoutingLocalDataSource = AlternativeRoutingLocalDataSourceImpl(dataStoreProvider)

    @Test
    fun `returns true when no preference is stored locally`() = runTest {
        // Given
        coEvery { preferences.get<Boolean>(any()) } returns null
        every { alternativeRoutingDataStoreMock.data } returns flowOf(preferences)

        // When
        alternativeRoutingLocalDataSource.observe().test {
            // Then
            Assert.assertEquals(AlternativeRoutingPreference(true).right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns locally stored preference from data store when available`() = runTest {
        // Given
        coEvery { preferences[booleanPreferencesKey("hasAlternativeRoutingPrefKey")] } returns false
        every { alternativeRoutingDataStoreMock.data } returns flowOf(preferences)

        // When
        alternativeRoutingLocalDataSource.observe().test {
            // Then
            Assert.assertEquals(AlternativeRoutingPreference(false).right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return error when an exception is thrown while observing preference`() = runTest {
        // Given
        every { alternativeRoutingDataStoreMock.data } returns flow { throw IOException() }

        // When
        alternativeRoutingLocalDataSource.observe().test {
            // Then
            Assert.assertEquals(PreferencesError.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return success when preference is saved`() = runTest {
        // Given
        val alternativeRoutingPreference = AlternativeRoutingPreference(isEnabled = true)
        coEvery { alternativeRoutingDataStoreMock.updateData(any()) } returns mockk()

        // When
        val result = alternativeRoutingLocalDataSource.save(alternativeRoutingPreference)

        // Then
        coVerify { alternativeRoutingDataStoreMock.updateData(any()) }
        Assert.assertEquals(Unit.right(), result)
    }

    @Test
    fun `should return failure when an exception is thrown while saving preference`() = runTest {
        // Given
        val alternativeRoutingPreference = AlternativeRoutingPreference(isEnabled = true)
        coEvery { alternativeRoutingDataStoreMock.updateData(any()) } throws IOException()

        // When
        val result = alternativeRoutingLocalDataSource.save(alternativeRoutingPreference)

        // Then
        Assert.assertEquals(PreferencesError.left(), result)
    }
}
