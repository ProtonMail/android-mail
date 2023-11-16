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
import ch.protonmail.android.mailsettings.domain.model.ExtendedNotificationPreference
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

internal class NotificationsSettingsRepositoryImplTest {

    private val preferences = mockk<Preferences>()
    private val extendedNotificationsDataStoreSpy = spyk<DataStore<Preferences>>()
    private val dataStoreProvider = mockk<MailSettingsDataStoreProvider> {
        every { this@mockk.notificationsDataStore } returns extendedNotificationsDataStoreSpy
    }

    private val extendedNotificationsDataStoreRepository: NotificationsSettingsRepositoryImpl =
        NotificationsSettingsRepositoryImpl(dataStoreProvider)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `returns true when no preference is stored locally`() = runTest {
        // Given
        coEvery { preferences.get<Boolean>(any()) } returns null
        every { extendedNotificationsDataStoreSpy.data } returns flowOf(preferences)

        // When
        extendedNotificationsDataStoreRepository.observeExtendedNotificationsSetting().test {
            // Then
            assertEquals(ExtendedNotificationPreference(true).right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns locally stored preference from data store when available`() = runTest {
        // Given
        coEvery { preferences[booleanPreferencesKey("extendedNotificationsPrefKey")] } returns true
        every { extendedNotificationsDataStoreSpy.data } returns flowOf(preferences)

        // When
        extendedNotificationsDataStoreRepository.observeExtendedNotificationsSetting().test {
            // Then
            assertEquals(ExtendedNotificationPreference(true).right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return error when an exception is thrown while observing preference`() = runTest {
        // Given
        every { extendedNotificationsDataStoreSpy.data } returns flow { throw IOException() }

        // When
        extendedNotificationsDataStoreRepository.observeExtendedNotificationsSetting().test {
            // Then
            assertEquals(PreferencesError.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return success when preference is updated`() = runTest {
        // Given
        val expectedResult = Unit.right()
        val updatedValue = false

        // When
        val result = extendedNotificationsDataStoreRepository.updateExtendedNotificationsSetting(updatedValue)

        // Then
        coVerify { extendedNotificationsDataStoreSpy.updateData(any()) }
        assertEquals(expectedResult, result)
    }

    @Test
    fun `should return failure when an exception is thrown while saving preference`() = runTest {
        // Given
        val expectedResult = PreferencesError.left()
        val updatedValue = false
        coEvery { extendedNotificationsDataStoreSpy.updateData(any()) } throws IOException()

        // When
        val result = extendedNotificationsDataStoreRepository.updateExtendedNotificationsSetting(updatedValue)

        // Then
        assertEquals(expectedResult, result)
    }
}
