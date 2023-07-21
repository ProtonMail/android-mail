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
import ch.protonmail.android.mailmessage.domain.entity.Participant
import ch.protonmail.android.testdata.contact.ContactIdTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.contact.domain.entity.ContactEmail
import me.proton.core.contact.domain.entity.ContactEmailId
import kotlin.test.Test
import kotlin.test.assertEquals

class ParticipantMapperTest {

    private val recipientUiModel = RecipientUiModel.Valid("test1@protonmail.com")
    private val participantMapper = ParticipantMapper()

    private val contacts = listOf(
        Contact(
            UserIdTestData.userId, ContactIdTestData.contactId1, "first contact",
            listOf(
                ContactEmail(
                    UserIdTestData.userId,
                    ContactEmailId("contact email id 1"),
                    "First name",
                    "test1@protonmail.com",
                    0,
                    0,
                    ContactIdTestData.contactId1,
                    "test1@protonmail.com",
                    emptyList()
                )
            )
        ),
        Contact(
            UserIdTestData.userId, ContactIdTestData.contactId2, "second contact",
            listOf(
                ContactEmail(
                    UserIdTestData.userId,
                    ContactEmailId("contact email id 2"),
                    "Second name",
                    "test2@protonmail.com",
                    0,
                    0,
                    ContactIdTestData.contactId1,
                    "test2@protonmail.com",
                    emptyList()
                )
            )
        )
    )

    @Test
    fun `valid recipient ui model is mapped to participant`() {
        // Given
        val expectedResult = Participant(
            address = "test1@protonmail.com",
            name = "First name",
            isProton = false
        )

        // When
        val result = participantMapper.recipientUiModelToParticipant(recipientUiModel, contacts)

        // Then
        assertEquals(expectedResult, result)
    }
}
