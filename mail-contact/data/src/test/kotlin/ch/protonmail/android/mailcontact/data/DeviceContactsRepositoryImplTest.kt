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
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Suppress("MaxLineLength")
class DeviceContactsRepositoryImplTest {

    private val columnIndexDisplayName = 1
    private val columnIndexEmail = 2
    private val displayNameColumnName = ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY
    private val emailAddressColumnName = ContactsContract.CommonDataKinds.Email.ADDRESS

    private lateinit var cursorMock: Cursor

    private val contentResolverMock = mockk<ContentResolver> { }

    private val contextMock = mockk<Context> {
        every { contentResolver } returns contentResolverMock
    }
    private val testDispatcherProvider = TestDispatcherProvider()

    private val deviceContactsRepository = DeviceContactsRepositoryImpl(
        contextMock,
        testDispatcherProvider
    )

    @BeforeTest
    fun setup() {
        cursorMock = getDefaultCursorMock()
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `when there are multiple matching contacts, they are emitted`() = runTest(testDispatcherProvider.Main) {
        // Given
        val query = "cont"

        expectCursorQuery(query)
        expectContactsCount(2)

        // When
        val actual = deviceContactsRepository.getDeviceContacts(query).getOrNull()

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
        val actual = deviceContactsRepository.getDeviceContacts(query).getOrNull()

        // Then
        assertNotNull(actual)
        assertTrue(actual.isEmpty())
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
        val actual = deviceContactsRepository.getDeviceContacts(query)

        // Then
        assertEquals(DeviceContactsRepository.DeviceContactsErrors.PermissionDenied.left(), actual)
        verify(exactly = 0) { cursorMock.getString(columnIndexDisplayName) }
        verify(exactly = 0) { cursorMock.getString(columnIndexEmail) }
    }

    @Test
    fun `when content resolver throws a generic exception, left is emitted`() = runTest(testDispatcherProvider.Main) {
        // Given
        val query = "cont"

        expectCursorQueryThrowsException()
        expectContactsCount(0)

        // When
        val actual = deviceContactsRepository.getDeviceContacts(query)

        // Then
        assertEquals(DeviceContactsRepository.DeviceContactsErrors.UnknownError.left(), actual)
        verify(exactly = 0) { cursorMock.getString(columnIndexDisplayName) }
        verify(exactly = 0) { cursorMock.getString(columnIndexEmail) }
    }

    @Test
    fun `when email address column is null, entries are not added`() = runTest(testDispatcherProvider.Main) {
        // Given
        val query = "cont"

        expectCursorQuery(query)
        expectContactsCount(2)

        every { cursorMock.getString(columnIndexEmail) } returns null

        // When
        val actual = deviceContactsRepository.getDeviceContacts(query).getOrNull()

        // Then
        assertNotNull(actual)
        assertTrue(actual.isEmpty())
        verify(exactly = 2) { cursorMock.getString(columnIndexEmail) }
        verify(exactly = 0) { cursorMock.getString(columnIndexDisplayName) }
    }

    @Test
    fun `when email address column index is null, entries are not added`() = runTest(testDispatcherProvider.Main) {
        // Given
        val query = "cont"

        expectCursorQuery(query)
        expectContactsCount(2)

        every { cursorMock.getColumnIndex(emailAddressColumnName) } returns -1

        // When
        val actual = deviceContactsRepository.getDeviceContacts(query).getOrNull()

        // Then
        assertNotNull(actual)
        assertTrue(actual.isEmpty())
        verify(exactly = 0) { cursorMock.getString(columnIndexEmail) }
        verify(exactly = 0) { cursorMock.getString(columnIndexDisplayName) }
    }

    @Test
    fun `when null display name column, fall back to the email address`() = runTest(testDispatcherProvider.Main) {
        // Given
        val query = "cont"

        expectCursorQuery(query)
        expectContactsCount(2)

        every { cursorMock.getString(columnIndexDisplayName) } returns null

        // When
        val actual = deviceContactsRepository.getDeviceContacts(query).getOrNull()

        // Then
        assertNotNull(actual)
        assertTrue(actual.size == 2)
        assertTrue(actual.all { it.name == it.email })
        verify(exactly = 2) { cursorMock.getString(columnIndexEmail) }
        verify(exactly = 2) { cursorMock.getString(columnIndexDisplayName) }
    }

    @Test
    fun `when null display name column index, fall back to the email address`() = runTest(testDispatcherProvider.Main) {
        // Given
        val query = "cont"

        expectCursorQuery(query)
        expectContactsCount(2)

        every { cursorMock.getColumnIndex(displayNameColumnName) } returns -1

        // When
        val actual = deviceContactsRepository.getDeviceContacts(query).getOrNull()

        // Then
        assertNotNull(actual)
        assertTrue(actual.size == 2)
        assertTrue(actual.all { it.name == it.email })
        verify(exactly = 2) { cursorMock.getString(columnIndexEmail) }
        verify(exactly = 0) { cursorMock.getString(columnIndexDisplayName) }
    }

    @Test
    fun `when cursor can't move to position, then no entry is added`() = runTest(testDispatcherProvider.Main) {
        // Given
        val query = "cont"

        expectCursorQuery(query)
        expectContactsCount(2)
        every { cursorMock.moveToPosition(1) } returns false

        // When
        val actual = deviceContactsRepository.getDeviceContacts(query).getOrNull()

        // Then
        assertNotNull(actual)
        assertTrue(actual.size == 1)
        verify(exactly = 1) { cursorMock.getString(columnIndexEmail) }
        verify(exactly = 1) { cursorMock.getString(columnIndexDisplayName) }
    }

    private fun getDefaultCursorMock() = mockk<Cursor> {
        every { getColumnIndex(displayNameColumnName) } returns columnIndexDisplayName
        every { getColumnIndex(emailAddressColumnName) } returns columnIndexEmail
        every { moveToPosition(any()) } returns true
        every { isNull(any<Int>()) } returns false
        every { getString(columnIndexDisplayName) } returns "display name"
        every { getString(columnIndexEmail) } returns "email"
        every { close() } just runs
    }

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

    private fun expectCursorQueryThrowsException() {
        every {
            contentResolverMock.query(any(), any(), any(), any(), any())
        } throws Exception("You shall not pass either")
    }

    private fun expectContactsCount(count: Int) {
        every { cursorMock.count } returns count
    }
}
