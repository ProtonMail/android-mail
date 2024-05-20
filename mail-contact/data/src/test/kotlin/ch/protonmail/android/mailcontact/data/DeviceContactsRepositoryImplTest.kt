package ch.protonmail.android.mailcontact.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import arrow.core.left
import ch.protonmail.android.mailcontact.domain.repository.DeviceContactsRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Assert
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suppress("MaxLineLength")
class DeviceContactsRepositoryImplTest {

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

    private val deviceContactsRepository = DeviceContactsRepositoryImpl(
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

    @org.junit.Test
    fun `when there are multiple matching contacts, they are emitted`() = runTest(testDispatcherProvider.Main) {
        // Given
        val query = "cont"

        expectCursorQuery(query)
        expectContactsCount(2)

        // When
        val actual = deviceContactsRepository.getDeviceContacts(query).getOrNull()

        // Then
        assertNotNull(actual)
        Assert.assertTrue(actual.size == 2)
        verify(exactly = 2) { cursorMock.getString(columnIndexDisplayName) }
        verify(exactly = 2) { cursorMock.getString(columnIndexEmail) }
    }

    @org.junit.Test
    fun `when there are no matching contacts, empty list is emitted`() = runTest(testDispatcherProvider.Main) {
        // Given
        val query = "cont"

        expectCursorQuery(query)
        expectContactsCount(0)

        // When
        val actual = deviceContactsRepository.getDeviceContacts(query).getOrNull()

        // Then
        assertNotNull(actual)
        Assert.assertTrue(actual.size == 0)
        verify(exactly = 0) { cursorMock.getString(columnIndexDisplayName) }
        verify(exactly = 0) { cursorMock.getString(columnIndexEmail) }
    }

    @org.junit.Test
    fun `when content resolver throws SecurityException, left is emitted`() = runTest(testDispatcherProvider.Main) {
        // Given
        val query = "cont"

        expectCursorQueryThrowsSecurityException()
        expectContactsCount(0)

        // When
        val actual = deviceContactsRepository.getDeviceContacts(query)

        // Then
        assertEquals(DeviceContactsRepository.DeviceContactsErrors.PermissionDenied.left(), actual)
        verify(exactly = 0) { cursorMock.getString(columnIndexDisplayName) }
        verify(exactly = 0) { cursorMock.getString(columnIndexEmail) }
    }

}
