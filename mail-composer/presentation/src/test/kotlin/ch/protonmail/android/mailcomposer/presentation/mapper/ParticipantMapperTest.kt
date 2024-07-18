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

import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.testdata.contact.ContactIdTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.contact.domain.entity.ContactEmail
import me.proton.core.contact.domain.entity.ContactEmailId
import kotlin.test.Test
import kotlin.test.assertEquals

class ParticipantMapperTest {

    private val recipientUiModel = RecipientUiModel.Valid("test1@protonmail.com")
    private val recipientUiModelNotInContacts = RecipientUiModel.Valid("test_not_in_contacts@protonmail.com")
    private val recipientUiModelEmptyNames = RecipientUiModel.Valid("test3@protonmail.com")
    private val participantMapper = ParticipantMapper()

    private val contacts = listOf(
        Contact(
            UserIdTestData.userId, ContactIdTestData.contactId1, "first contact",
            listOf(
                ContactEmail(
                    UserIdTestData.userId,
                    ContactEmailId("contact email id 1"),
                    "First name from contact email",
                    "test1+alias@protonmail.com",
                    0,
                    0,
                    ContactIdTestData.contactId1,
                    "test1@protonmail.com",
                    emptyList(),
                    true,
                    lastUsedTime = 0
                )
            )
        ),
        Contact(
            UserIdTestData.userId, ContactIdTestData.contactId2, "",
            listOf(
                ContactEmail(
                    UserIdTestData.userId,
                    ContactEmailId("contact email id 2"),
                    "",
                    "test2@protonmail.com",
                    0,
                    0,
                    ContactIdTestData.contactId2,
                    "test2@protonmail.com",
                    emptyList(),
                    false,
                    lastUsedTime = 0
                ),
                ContactEmail(
                    UserIdTestData.userId,
                    ContactEmailId("contact email id 3"),
                    "",
                    "test3@protonmail.com",
                    0,
                    0,
                    ContactIdTestData.contactId1,
                    "test3@protonmail.com",
                    emptyList(),
                    false,
                    lastUsedTime = 0
                )
            )
        )
    )

    @Test
    fun `valid recipient ui model is mapped to participant, name from ContactEmail`() {
        // Given
        val expectedResult = Participant(
            address = "test1@protonmail.com",
            name = "First name from contact email",
            isProton = true
        )

        // When
        val result = participantMapper.recipientUiModelToParticipant(recipientUiModel, contacts)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `valid recipient ui model is mapped to participant, Contact names are empty, fallback name to email address`() {
        // Given
        val expectedResult = Participant(
            address = "test_not_in_contacts@protonmail.com",
            name = "test_not_in_contacts@protonmail.com",
            isProton = false
        )

        // When
        val result = participantMapper.recipientUiModelToParticipant(recipientUiModelNotInContacts, contacts)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `valid recipient ui model is mapped to participant, not found in Contacts, fallback name to email address`() {
        // Given
        val expectedResult = Participant(
            address = "test3@protonmail.com",
            name = "test3@protonmail.com",
            isProton = false
        )

        // When
        val result = participantMapper.recipientUiModelToParticipant(recipientUiModelEmptyNames, contacts)

        // Then
        assertEquals(expectedResult, result)
    }
}
