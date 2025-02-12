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

package ch.protonmail.android.mailcomposer.presentation.mapper

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcontact.domain.model.DeviceContact
import ch.protonmail.android.mailcontact.domain.usecase.SearchContacts
import ch.protonmail.android.mailcontact.domain.usecase.SearchDeviceContacts
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.testdata.contact.ContactTestData
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ComposerParticipantMapperTest {

    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()
    private val searchContacts = mockk<SearchContacts>()
    private val searchDeviceContacts = mockk<SearchDeviceContacts>()

    private lateinit var participantMapper: ComposerParticipantMapper

    private val validRecipient = RecipientUiModel.Valid("test@example.com")

    @BeforeTest
    fun setup() {
        participantMapper = ComposerParticipantMapper(
            observePrimaryUserId,
            searchContacts,
            searchDeviceContacts
        )
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return a participant with name = address when unable to get the primary user id`() = runTest {
        // Given
        every { observePrimaryUserId.invoke() } returns flowOf()

        val expectedParticipant = Participant(
            address = validRecipient.address,
            name = validRecipient.address
        )

        // When
        val actual = participantMapper.recipientUiModelToParticipant(validRecipient)

        // Then
        assertEquals(expectedParticipant, actual)
        coVerify { searchContacts wasNot called }
        coVerify { searchDeviceContacts wasNot called }
    }

    @Test
    fun `should return a participant with name = address when address is unknown across all sources`() = runTest {
        // Given
        expectValidUserId()
        expectNoProtonContact(validRecipient.address)
        expectNoDeviceContact(validRecipient.address)

        val expectedParticipant = Participant(
            address = validRecipient.address,
            name = validRecipient.address
        )

        // When
        val actual = participantMapper.recipientUiModelToParticipant(validRecipient)

        // Then
        assertEquals(expectedParticipant, actual)
        coVerifySequence {
            searchContacts.invoke(userId, validRecipient.address)
            searchDeviceContacts.invoke(validRecipient.address)
        }
    }

    @Test
    fun `should return a participant with the account contacts details when found`() = runTest {
        // Given
        expectValidUserId()
        expectProtonContact(validRecipient.address, "contact")

        val expectedParticipant = Participant(
            address = validRecipient.address,
            name = "contact"
        )

        // When
        val actual = participantMapper.recipientUiModelToParticipant(validRecipient)

        // Then
        assertEquals(expectedParticipant, actual)
        coVerify(exactly = 1) { searchContacts.invoke(userId, validRecipient.address) }
        coVerify { searchDeviceContacts wasNot called }
    }

    @Test
    fun `should hit the cache when querying the same contact email twice`() = runTest {
        // Given
        expectValidUserId()
        expectProtonContact(validRecipient.address, "contact")

        val expectedParticipant = Participant(
            address = validRecipient.address,
            name = "contact"
        )

        // When
        val firstQueryResult = participantMapper.recipientUiModelToParticipant(validRecipient)
        val secondQueryResult = participantMapper.recipientUiModelToParticipant(validRecipient)

        // Then
        assertEquals(expectedParticipant, firstQueryResult)
        assertEquals(expectedParticipant, secondQueryResult)
        coVerify(exactly = 1) { searchContacts.invoke(userId, validRecipient.address) }
        coVerify { searchDeviceContacts wasNot called }
    }

    @Test
    fun `should query the device contacts when the email is not known in the account contacts`() = runTest {
        // Given
        expectValidUserId()
        expectNoProtonContact(validRecipient.address)
        expectDeviceContact(validRecipient.address, "contact")

        val expectedParticipant = Participant(
            address = validRecipient.address,
            name = "contact"
        )

        // When
        val actual = participantMapper.recipientUiModelToParticipant(validRecipient)

        // Then
        assertEquals(expectedParticipant, actual)
        coVerifySequence {
            searchContacts.invoke(userId, validRecipient.address)
            searchDeviceContacts.invoke(validRecipient.address)
        }
    }

    private fun expectValidUserId() {
        every { observePrimaryUserId.invoke() } returns flowOf(userId)
    }

    private fun expectProtonContact(address: String, name: String) {

        val contact = ContactTestData.buildContactWith(
            userId,
            contactEmails = listOf(ContactTestData.buildContactEmailWith(userId, name = name, address = address))
        )

        coEvery { searchContacts(userId, address) } returns flowOf(listOf<Contact>(contact).right())
    }

    private fun expectNoProtonContact(address: String) {
        coEvery { searchContacts(userId, address) } returns flowOf(emptyList<Contact>().right())
    }

    private fun expectDeviceContact(address: String, name: String) {
        coEvery { searchDeviceContacts(address) } returns listOf(DeviceContact(name, address)).right()
    }

    private fun expectNoDeviceContact(address: String) {
        coEvery { searchDeviceContacts(address) } returns emptyList<DeviceContact>().right()
    }

    private companion object {

        val userId = UserId("random-id")
    }
}
