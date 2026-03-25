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

package ch.protonmail.android.mailevents.data.local

import java.io.IOException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import arrow.core.left
import ch.protonmail.android.mailcommon.domain.model.DataError
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class MailEventsDataSourceImplTest {

    private val asidKey = stringPreferencesKey(EventsDataStoreProvider.ASID_KEY)
    private val installEventSentKey = booleanPreferencesKey(EventsDataStoreProvider.INSTALL_EVENT_SENT_KEY)

    private val preferences = mockk<Preferences>()
    private val mutablePreferences = mockk<MutablePreferences>(relaxed = true)
    private val eventsDataStoreMock = mockk<DataStore<Preferences>>()
    private val dataStoreProvider = mockk<EventsDataStoreProvider> {
        every { eventsDataStore } returns eventsDataStoreMock
    }

    private val asidLocalDataSource = MailEventsDataSourceImpl(dataStoreProvider)

    @Test
    fun `should return existing ASID when already stored`() = runTest {
        // Given
        val existingAsid = "existing-asid-12345"
        every { preferences[asidKey] } returns existingAsid
        every { eventsDataStoreMock.data } returns flowOf(preferences)

        // When
        val asid = asidLocalDataSource.getOrCreateAsid().getOrNull()

        // Then
        assertEquals(existingAsid, asid)
    }

    @Test
    fun `should create new ASID when none exists`() = runTest {
        // Given
        every { preferences[asidKey] } returns null
        every { eventsDataStoreMock.data } returns flowOf(preferences)
        val savedAsidSlot = slot<String>()
        coEvery { eventsDataStoreMock.updateData(any()) } coAnswers {
            val transform = firstArg<suspend (MutablePreferences) -> MutablePreferences>()
            every { mutablePreferences[asidKey] = capture(savedAsidSlot) } returns Unit
            transform(mutablePreferences)
            preferences
        }

        // When
        val asid = asidLocalDataSource.getOrCreateAsid().getOrNull()

        // Then
        assertNotNull(asid)
        assertTrue(asid.isNotBlank())
        assertTrue(asid.matches(Regex("[a-f0-9-]{36}")))

    }

    @Test
    fun `should return error when data store read fails`() = runTest {
        // Given
        every { eventsDataStoreMock.data } returns kotlinx.coroutines.flow.flow { throw IOException() }

        // When
        val result = asidLocalDataSource.getOrCreateAsid()

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
    }

    @Test
    fun `should return error when data store write fails`() = runTest {
        // Given
        every { preferences[asidKey] } returns null
        every { eventsDataStoreMock.data } returns flowOf(preferences)
        coEvery { eventsDataStoreMock.updateData(any()) } throws IOException()

        // When
        val result = asidLocalDataSource.getOrCreateAsid()

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
    }

    @Test
    fun `should return false when install event has not been sent`() = runTest {
        // Given
        every { preferences[installEventSentKey] } returns null
        every { eventsDataStoreMock.data } returns flowOf(preferences)

        // When
        val result = asidLocalDataSource.hasInstallEventBeenSent()

        // Then
        assertFalse(result)
    }

    @Test
    fun `should return true when install event has been sent`() = runTest {
        // Given
        every { preferences[installEventSentKey] } returns true
        every { eventsDataStoreMock.data } returns flowOf(preferences)

        // When
        val result = asidLocalDataSource.hasInstallEventBeenSent()

        // Then
        assertTrue(result)
    }

    @Test
    fun `should return false when data store read fails for install event check`() = runTest {
        // Given
        every { eventsDataStoreMock.data } returns kotlinx.coroutines.flow.flow { throw IOException() }

        // When
        val result = asidLocalDataSource.hasInstallEventBeenSent()

        // Then
        assertFalse(result)
    }

    @Test
    fun `should return success when marking install event as sent`() = runTest {
        // Given
        coEvery { eventsDataStoreMock.updateData(any()) } returns preferences

        // When
        val result = asidLocalDataSource.markInstallEventSent()

        // Then
        assertTrue(result.isRight())
    }

    @Test
    fun `should return error when marking install event fails`() = runTest {
        // Given
        coEvery { eventsDataStoreMock.updateData(any()) } throws IOException()

        // When
        val result = asidLocalDataSource.markInstallEventSent()

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
    }
}
