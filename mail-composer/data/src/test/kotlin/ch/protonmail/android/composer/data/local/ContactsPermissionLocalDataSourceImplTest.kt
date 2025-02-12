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

package ch.protonmail.android.composer.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

internal class ContactsPermissionLocalDataSourceImplTest {
    private val preferences = mockk<Preferences>()
    private val contactsPermissionDataStoreSpy = spyk<DataStore<Preferences>> {
        every { data } returns flowOf(preferences)
    }
    private val dataStoreProviderMock = mockk<ContactsPermissionDataStoreProvider> {
        every { contactsPermissionsStore } returns contactsPermissionDataStoreSpy
    }

    private val dataSource = ContactsPermissionLocalDataSourceImpl(dataStoreProviderMock)

    @Test
    fun `should return data when present`() = runTest {
        // Given
        val expectedState = false
        every { preferences[booleanPreferencesKey(SHOULD_STOP_SHOWING_PERMISSION_DIALOG)] } returns expectedState

        // When
        val actual = dataSource.observePermissionDenied().first()

        // Then
        assertEquals(expectedState.right(), actual)
    }

    @Test
    fun `should return an error when data is not present`() = runTest {
        every { preferences[booleanPreferencesKey(SHOULD_STOP_SHOWING_PERMISSION_DIALOG)] } returns null

        // When
        val actual = dataSource.observePermissionDenied().first()

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

    @Test
    fun `should save the denied state when invoked`() = runTest {
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        val mutablePreferences = mockk<MutablePreferences>()

        every { preferences.toMutablePreferences() } returns mutablePreferences
        every { mutablePreferences[any<Preferences.Key<Boolean>>()] = any() } returns Unit

        // When
        dataSource.trackPermissionDeniedEvent()

        // Then
        coVerify {
            contactsPermissionDataStoreSpy.updateData(capture(transformSlot))
        }

        transformSlot.captured.invoke(preferences)

        verify {
            mutablePreferences[booleanPreferencesKey(SHOULD_STOP_SHOWING_PERMISSION_DIALOG)] = true
        }
    }

    private companion object {
        const val SHOULD_STOP_SHOWING_PERMISSION_DIALOG = "HasDeniedContactsPermission"
    }
}
