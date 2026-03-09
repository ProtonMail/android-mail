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

package ch.protonmail.android.mailcontact.data.local

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcontact.data.mapper.ContactGroupItemMapper
import ch.protonmail.android.mailcontact.data.mapper.ContactItemMapper
import ch.protonmail.android.mailcontact.data.mapper.ContactItemTypeMapper
import ch.protonmail.android.mailcontact.data.mapper.ContactSuggestionsMapper
import ch.protonmail.android.mailcontact.data.mapper.DeviceContactsMapper
import ch.protonmail.android.mailcontact.data.mapper.DeviceContactsWithSignatureMapper
import ch.protonmail.android.mailcontact.data.mapper.GroupedContactsMapper
import ch.protonmail.android.mailcontact.data.model.LocalDeviceContactsWithSignature
import ch.protonmail.android.mailcontact.data.usecase.CreateRustContactWatcher
import ch.protonmail.android.mailcontact.data.usecase.RustDeleteContact
import ch.protonmail.android.mailcontact.data.usecase.RustGetContactDetails
import ch.protonmail.android.mailcontact.data.usecase.RustGetContactSuggestions
import ch.protonmail.android.mailcontact.domain.model.ContactSuggestionQuery
import ch.protonmail.android.mailcontact.domain.model.DeviceContactsWithSignature
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.contact.ContactTestData
import ch.protonmail.android.testdata.contact.rust.LocalContactSuggestionTestData
import ch.protonmail.android.testdata.contact.rust.LocalContactTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import uniffi.mail_uniffi.ActionError
import uniffi.mail_uniffi.ActionErrorReason
import uniffi.mail_uniffi.ContactDetailCard
import uniffi.mail_uniffi.ContactsLiveQueryCallback
import uniffi.mail_uniffi.VoidActionResult
import uniffi.mail_uniffi.WatchedContactList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RustContactDataSourceImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private val testCoroutineScope = CoroutineScope(mainDispatcherRule.testDispatcher)

    private val mockWatcher = mockk<WatchedContactList>(relaxed = true)

    private val userSessionRepository = mockk<UserSessionRepository>()
    private val createRustContactWatcher = mockk<CreateRustContactWatcher>()
    private val rustDeleteContact = mockk<RustDeleteContact>()
    private val rustGetContactSuggestions = mockk<RustGetContactSuggestions>()
    private val contactListUpdatedCallbackSlot = slot<ContactsLiveQueryCallback>()
    private val rustGetContactDetails = mockk<RustGetContactDetails>()

    private val contactItemMapper = ContactItemMapper()
    private val contactGroupItemMapper = ContactGroupItemMapper()
    private val contactItemTypeMapper = ContactItemTypeMapper(contactItemMapper, contactGroupItemMapper)
    private val groupedContactsMapper = GroupedContactsMapper(contactItemTypeMapper)
    private val contactSuggestionsMapper = ContactSuggestionsMapper()
    private val deviceContactsMapper = DeviceContactsWithSignatureMapper(DeviceContactsMapper())

    private val rustContactDataSource = RustContactDataSourceImpl(
        userSessionRepository,
        groupedContactsMapper,
        contactSuggestionsMapper,
        deviceContactsMapper,
        createRustContactWatcher,
        rustDeleteContact,
        rustGetContactSuggestions,
        rustGetContactDetails,
        testCoroutineScope
    )

    @Test
    fun `observing all contacts emits contact metadata list when grouped contacts are loaded`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val session = mockk<MailUserSessionWrapper>()
        val localGroupedContactList = listOf(
            LocalContactTestData.groupedContactsByA, LocalContactTestData.groupedContactsByB
        )
        val expectedContacts = localGroupedContactList
            .map { groupedContactsMapper.toGroupedContacts(it) }
            .flatMap { it.contacts }

        coEvery { userSessionRepository.getUserSession(userId) } returns session
        coEvery {
            createRustContactWatcher(session, capture(contactListUpdatedCallbackSlot))
        } returns mockWatcher.right()
        every { mockWatcher.contactList } returns localGroupedContactList

        // When
        val result = rustContactDataSource.observeAllContacts(userId).first()

        // Then
        assertEquals(expectedContacts.right(), result)
    }

    @Test
    fun `observeAllContacts should return error when session is null`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        val result = rustContactDataSource.observeAllContacts(userId).first()

        // Then
        assertEquals(GetContactError.left(), result)
    }

    @Test
    fun `observeAllContacts should return error when rust fails to create the contacts watcher`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val session = mockk<MailUserSessionWrapper>()
        coEvery { userSessionRepository.getUserSession(userId) } returns session
        coEvery {
            createRustContactWatcher(session, capture(contactListUpdatedCallbackSlot))
        } returns DataError.Local.CryptoError.left()

        // When
        val result = rustContactDataSource.observeAllContacts(userId).first()

        // Then
        assertEquals(GetContactError.left(), result)
    }

    @Test
    fun `observeAllGroupedContacts should initialize watcher and emit contact groups`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val session = mockk<MailUserSessionWrapper>()
        val localGroupedContactList = listOf(
            LocalContactTestData.groupedContactsByA, LocalContactTestData.groupedContactsByB
        )
        val expectedGroupedContactList = localGroupedContactList
            .map { groupedContactsMapper.toGroupedContacts(it) }

        coEvery { userSessionRepository.getUserSession(userId) } returns session
        coEvery {
            createRustContactWatcher(session, capture(contactListUpdatedCallbackSlot))
        } returns mockWatcher.right()
        every { mockWatcher.contactList } returns localGroupedContactList

        // When
        val result = rustContactDataSource.observeAllGroupedContacts(userId).first()

        // Then
        assertEquals(expectedGroupedContactList.right(), result)
    }

    @Test
    fun `deleteContact should call rustDeleteContact when session is available`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val contactId = LocalContactTestData.contactId1
        val session = mockk<MailUserSessionWrapper>()
        coEvery { userSessionRepository.getUserSession(userId) } returns session
        coEvery { rustDeleteContact(session, contactId) } returns VoidActionResult.Ok

        // When
        val result = rustContactDataSource.deleteContact(userId, contactId)

        // Then
        assertTrue(result.isRight())
        coVerify { rustDeleteContact(session, contactId) }
    }

    @Test
    fun `deleteContact should return error when session is null`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val contactId = LocalContactTestData.contactId1
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        val result = rustContactDataSource.deleteContact(userId, contactId)

        // Then
        assertEquals(DataError.Local.NoUserSession.left(), result)
    }

    @Test
    fun `deleteContact should return error when rust delete contact fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val contactId = LocalContactTestData.contactId1
        val session = mockk<MailUserSessionWrapper>()
        coEvery { userSessionRepository.getUserSession(userId) } returns session
        coEvery { rustDeleteContact(session, contactId) } returns
            VoidActionResult.Error(ActionError.Reason(ActionErrorReason.UNKNOWN_MESSAGE))

        // When
        val result = rustContactDataSource.deleteContact(userId, contactId)

        // Then
        assertEquals(DataError.Local.NotFound.left(), result)
    }

    @Test
    fun `does not create a new watcher for each call to observe grouped contacts`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val session = mockk<MailUserSessionWrapper>()
        val callbackSlot = slot<ContactsLiveQueryCallback>()
        coEvery { userSessionRepository.getUserSession(userId) } returns session
        coEvery { createRustContactWatcher(session, capture(callbackSlot)) } returns mockWatcher.right()

        // First call returns one callback flow
        val firstFlow = rustContactDataSource.observeAllGroupedContacts(userId).first()

        // Second call returns a new callback flow
        val secondFlow = rustContactDataSource.observeAllGroupedContacts(userId).first()

        // Then
        assertSame(firstFlow, secondFlow)
        coVerify(exactly = 1) { createRustContactWatcher(session, any()) }
    }

    @Test
    fun `get contact suggestions returns suggestions from rust contact suggestions`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val query = ContactSuggestionQuery("test")
        val localDeviceContacts = LocalDeviceContactsWithSignature(contacts = emptyList(), signature = 0L)
        val deviceContacts = DeviceContactsWithSignature.Empty
        val session = mockk<MailUserSessionWrapper>()
        val localContactSuggestions = listOf(
            LocalContactSuggestionTestData.contactSuggestion,
            LocalContactSuggestionTestData.contactGroupSuggestion
        )
        val expected = listOf(ContactTestData.contactSuggestion, ContactTestData.contactGroupSuggestion)
        coEvery { userSessionRepository.getUserSession(userId) } returns session
        coEvery {
            rustGetContactSuggestions(userId, session, localDeviceContacts, query.value)
        } returns localContactSuggestions.right()

        // When
        val result = rustContactDataSource.getContactSuggestions(userId, deviceContacts, query)

        // Then
        assertEquals(expected.right(), result)
    }

    @Test
    fun `get contact suggestions should return error when session is null`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val query = ContactSuggestionQuery("test")
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        val result = rustContactDataSource.getContactSuggestions(userId, DeviceContactsWithSignature.Empty, query)

        // Then
        assertEquals(DataError.Local.NoUserSession.left(), result)
    }

    @Test
    fun `should get contact details when session is available`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val contactId = LocalContactTestData.contactId1
        val session = mockk<MailUserSessionWrapper>()
        val contactDetailCard = mockk<ContactDetailCard>(relaxed = true)
        coEvery { userSessionRepository.getUserSession(userId) } returns session
        coEvery { rustGetContactDetails(session, contactId) } returns contactDetailCard.right()

        // When
        val result = rustContactDataSource.getContactDetails(userId, contactId)

        // Then
        assertTrue(result.isRight())
        coVerify { rustGetContactDetails(session, contactId) }
    }

    @Test
    fun `getting contact details should return error when session is null`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val contactId = LocalContactTestData.contactId1
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        val result = rustContactDataSource.getContactDetails(userId, contactId)

        // Then
        assertEquals(DataError.Local.NoUserSession.left(), result)
    }

    @Test
    fun `getting contact details should return error when use case fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val contactId = LocalContactTestData.contactId1
        val session = mockk<MailUserSessionWrapper>()
        coEvery { userSessionRepository.getUserSession(userId) } returns session
        coEvery { rustGetContactDetails(session, contactId) } returns DataError.Local.CryptoError.left()

        // When
        val result = rustContactDataSource.getContactDetails(userId, contactId)

        // Then
        assertEquals(DataError.Local.CryptoError.left(), result)
    }

    @Test
    fun `preload contact suggestions when session is available`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val session = mockk<MailUserSessionWrapper>()
        val deviceContacts = DeviceContactsWithSignature.Empty
        val local = LocalDeviceContactsWithSignature(contacts = emptyList(), signature = 0L)

        coEvery { userSessionRepository.getUserSession(userId) } returns session
        coEvery { rustGetContactSuggestions.preload(userId, session, local) } returns Unit.right()

        // When
        val result = rustContactDataSource.preloadContactSuggestions(userId, deviceContacts)

        // Then
        assertEquals(Unit.right(), result)
        coVerify(exactly = 1) { rustGetContactSuggestions.preload(userId, session, local) }
    }
}

