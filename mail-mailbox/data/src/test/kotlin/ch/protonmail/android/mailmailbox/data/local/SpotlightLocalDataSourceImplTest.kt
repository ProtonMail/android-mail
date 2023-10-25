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

package ch.protonmail.android.mailmailbox.data.local

import java.io.IOException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailmailbox.data.MailMailboxDataStoreProvider
import ch.protonmail.android.mailmailbox.data.repository.local.SpotlightLocalDataSourceImpl
import ch.protonmail.android.mailmailbox.domain.model.SpotlightPreference
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class SpotlightLocalDataSourceImplTest {

    private val preferences = mockk<Preferences>()
    private val spotlightDataStoreMock = mockk<DataStore<Preferences>>()
    private val dataStoreProvider = mockk<MailMailboxDataStoreProvider> {
        every { this@mockk.spotlightDataStore } returns spotlightDataStoreMock
    }

    private val spotlightLocalDataSource = SpotlightLocalDataSourceImpl(dataStoreProvider)

    @Test
    fun `returns true when no preference is stored locally`() = runTest {
        // Given
        coEvery { preferences.get<Boolean>(any()) } returns null
        every { spotlightDataStoreMock.data } returns flowOf(preferences)

        // When
        spotlightLocalDataSource.observe().test {
            // Then
            Assert.assertEquals(SpotlightPreference(true).right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns locally stored preference from data store when available`() = runTest {
        // Given
        coEvery { preferences[booleanPreferencesKey("hasSpotlightPrefKey")] } returns false
        every { spotlightDataStoreMock.data } returns flowOf(preferences)

        // When
        spotlightLocalDataSource.observe().test {
            // Then
            Assert.assertEquals(SpotlightPreference(false).right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return error when an exception is thrown while observing preference`() = runTest {
        // Given
        every { spotlightDataStoreMock.data } returns flow { throw IOException() }

        // When
        spotlightLocalDataSource.observe().test {
            // Then
            Assert.assertEquals(PreferencesError.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return success when preference is saved`() = runTest {
        // Given
        val spotlightPreference = SpotlightPreference(display = true)
        coEvery { spotlightDataStoreMock.updateData(any()) } returns mockk()

        // When
        val result = spotlightLocalDataSource.save(spotlightPreference)

        // Then
        coVerify { spotlightDataStoreMock.updateData(any()) }
        Assert.assertEquals(Unit.right(), result)
    }

    @Test
    fun `should return failure when an exception is thrown while saving preference`() = runTest {
        // Given
        val spotlightPreference = SpotlightPreference(display = true)
        coEvery { spotlightDataStoreMock.updateData(any()) } throws IOException()

        // When
        val result = spotlightLocalDataSource.save(spotlightPreference)

        // Then
        Assert.assertEquals(PreferencesError.left(), result)
    }
}
