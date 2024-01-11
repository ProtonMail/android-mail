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
import ch.protonmail.android.mailmailbox.domain.model.StorageLimitPreference
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class StorageLimitLocalDataSourceImplTest {

    private val preferences = mockk<Preferences>()
    private val storageLimitDataStore = mockk<DataStore<Preferences>>()
    private val dataStoreProvider = mockk<MailMailboxDataStoreProvider> {
        every { this@mockk.storageLimitPrefDataStore } returns storageLimitDataStore
    }

    private val storageLimitLocalDataSource = StorageLimitLocalDataSourceImpl(dataStoreProvider)
    private val defaultValue = StorageLimitLocalDataSourceImpl.DEFAULT_VALUE
    private val firstLimitWarningConfirmedPrefKey =
        booleanPreferencesKey(StorageLimitLocalDataSourceImpl.FIRST_LIMIT_WARNING_CONFIRMED_PREF_KEY)
    private val secondLimitWarningConfirmedPrefKey = booleanPreferencesKey(
        StorageLimitLocalDataSourceImpl.SECOND_LIMIT_WARNING_CONFIRMED_PREF_KEY
    )

    @Test
    fun `returns default values when no preference is stored locally`() = runTest {
        // Given
        val expectedValue = StorageLimitPreference(defaultValue, defaultValue).right()
        coEvery { preferences.get<Boolean>(any()) } returns null
        every { storageLimitDataStore.data } returns flowOf(preferences)

        // When
        storageLimitLocalDataSource.observe().test {
            // Then
            Assert.assertEquals(expectedValue, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns locally stored preference from data store when available`() = runTest {
        // Given
        val expectedValue = StorageLimitPreference(
            firstLimitWarningConfirmed = false,
            secondLimitWarningConfirmed = true
        ).right()
        coEvery { preferences[firstLimitWarningConfirmedPrefKey] } returns false
        coEvery { preferences[secondLimitWarningConfirmedPrefKey] } returns true

        every { storageLimitDataStore.data } returns flowOf(preferences)

        // When
        storageLimitLocalDataSource.observe().test {
            // Then
            Assert.assertEquals(expectedValue, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return error when an exception is thrown while observing preference`() = runTest {
        // Given
        every { storageLimitDataStore.data } returns flow { throw IOException() }

        // When
        storageLimitLocalDataSource.observe().test {
            // Then
            Assert.assertEquals(PreferencesError.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return success when preference is saved`() = runTest {
        // Given
        coEvery { storageLimitDataStore.updateData(any()) } returns mockk()

        // When
        val result = storageLimitLocalDataSource.saveFirstLimitWarningPreference(true)

        // Then
        coVerify { storageLimitDataStore.updateData(any()) }
        Assert.assertEquals(Unit.right(), result)
    }

    @Test
    fun `should return failure when an exception is thrown while saving preference`() = runTest {
        // Given
        coEvery { storageLimitDataStore.updateData(any()) } throws IOException()

        // When
        val result = storageLimitLocalDataSource.saveSecondLimitWarningPreference(true)

        // Then
        Assert.assertEquals(PreferencesError.left(), result)
    }
}
