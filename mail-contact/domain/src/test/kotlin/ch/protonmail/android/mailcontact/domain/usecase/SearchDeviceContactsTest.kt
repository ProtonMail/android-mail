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

package ch.protonmail.android.mailcontact.domain.usecase

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import arrow.core.left
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SearchDeviceContactsTest {

    private val columnIndexDisplayName = 1
    private val columnIndexEmail = 2

    private val cursorMock = mockk<Cursor> {
        every {
            getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY)
        } returns columnIndexDisplayName
        every { getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS) } returns columnIndexEmail
        every { moveToPosition(any()) } returns true
        every { getString(columnIndexDisplayName) } returns "display name"
        every { getString(columnIndexEmail) } returns "email"
        every { close() } just runs
    }

    private val contentResolverMock = mockk<ContentResolver> { }

    private val contextMock = mockk<Context> {
        every { contentResolver } returns contentResolverMock
    }
    private val testDispatcherProvider = TestDispatcherProvider()

    private val searchDeviceContacts = SearchDeviceContacts(
        contextMock,
        testDispatcherProvider
    )

    private fun expectCursorQuery(query: String) {
        every {
            contentResolverMock.query(any(), any(), any(), arrayOf("%$query%", "%$query%", "%$query%"), any())
        } returns cursorMock
    }

    private fun expectCursorQueryThrowsSecurityException() {
        every {
            contentResolverMock.query(any(), any(), any(), any(), any())
        } throws SecurityException("You shall not pass")
    }

    private fun expectContactsCount(count: Int) {
        every { cursorMock.count } returns count
    }

    @Test
    fun `when there are multiple matching contacts, they are emitted`() = runTest(testDispatcherProvider.Main) {
        // Given
        val query = "cont"

        expectCursorQuery(query)
        expectContactsCount(2)

        // When
        val actual = searchDeviceContacts(query).getOrNull()

        // Then
        assertNotNull(actual)
        assertTrue(actual.size == 2)
        verify(exactly = 2) { cursorMock.getString(columnIndexDisplayName) }
        verify(exactly = 2) { cursorMock.getString(columnIndexEmail) }
    }

    @Test
    fun `when there are no matching contacts, empty list is emitted`() = runTest(testDispatcherProvider.Main) {
        // Given
        val query = "cont"

        expectCursorQuery(query)
        expectContactsCount(0)

        // When
        val actual = searchDeviceContacts(query).getOrNull()

        // Then
        assertNotNull(actual)
        assertTrue(actual.size == 0)
        verify(exactly = 0) { cursorMock.getString(columnIndexDisplayName) }
        verify(exactly = 0) { cursorMock.getString(columnIndexEmail) }
    }

    @Test
    fun `when content resolver throws SecurityException, left is emitted`() = runTest(testDispatcherProvider.Main) {
        // Given
        val query = "cont"

        expectCursorQueryThrowsSecurityException()
        expectContactsCount(0)

        // When
        val actual = searchDeviceContacts(query)

        // Then
        assertEquals(GetContactError.left(), actual)
        verify(exactly = 0) { cursorMock.getString(columnIndexDisplayName) }
        verify(exactly = 0) { cursorMock.getString(columnIndexEmail) }
    }
}
