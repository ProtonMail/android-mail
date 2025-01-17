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

package ch.protonmail.android.mailnotifications.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationPermissionLocalDataSourceImplTest {

    private val preferences = mockk<Preferences>()
    private val notificationPermissionStoreSpy = spyk<DataStore<Preferences>> {
        every { data } returns flowOf(preferences)
    }
    private val dataStoreProviderMock = mockk<DataStoreProvider> {
        every { notificationPermissionStore } returns notificationPermissionStoreSpy
    }

    private val notificationPermissionLocalDataSource = NotificationPermissionLocalDataSourceImpl(dataStoreProviderMock)

    @Test
    fun `should get notification permission timestamp`() = runTest {
        // Given
        val timestamp = 123L
        every { preferences[longPreferencesKey(NOTIFICATION_PERMISSION_TIMESTAMP_KEY)] } returns timestamp

        // When
        val actual = notificationPermissionLocalDataSource.getNotificationPermissionTimestamp()

        // Then
        assertEquals(timestamp.right(), actual)
    }

    @Test
    fun `should return error when notification permission timestamp is not saved`() = runTest {
        // Given
        every { preferences[longPreferencesKey(NOTIFICATION_PERMISSION_TIMESTAMP_KEY)] } returns null

        // When
        val actual = notificationPermissionLocalDataSource.getNotificationPermissionTimestamp()

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

    @Test
    fun `should save notification permission timestamp`() = runTest {
        // Given
        val timestamp = 123L

        // When
        notificationPermissionLocalDataSource.saveNotificationPermissionTimestamp(timestamp)

        // Then
        coVerify { notificationPermissionStoreSpy.updateData(any()) }
    }

    @Test
    fun `should get stop showing permission dialog`() = runTest {
        // Given
        every { preferences[booleanPreferencesKey(SHOULD_STOP_SHOWING_PERMISSION_DIALOG)] } returns true

        // When
        val actual = notificationPermissionLocalDataSource.getShouldStopShowingPermissionDialog()

        // Then
        assertEquals(true.right(), actual)
    }

    @Test
    fun `should return error when stop showing permission dialog is not saved`() = runTest {
        // Given
        every { preferences[booleanPreferencesKey(SHOULD_STOP_SHOWING_PERMISSION_DIALOG)] } returns null

        // When
        val actual = notificationPermissionLocalDataSource.getShouldStopShowingPermissionDialog()

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

    @Test
    fun `should save stop showing permission dialog`() = runTest {
        // When
        notificationPermissionLocalDataSource.saveShouldStopShowingPermissionDialog(
            shouldStopShowingPermissionDialog = true
        )

        // Then
        coVerify { notificationPermissionStoreSpy.updateData(any()) }
    }
}
