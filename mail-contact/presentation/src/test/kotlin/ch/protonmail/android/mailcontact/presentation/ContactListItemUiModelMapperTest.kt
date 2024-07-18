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

package ch.protonmail.android.mailcontact.presentation

import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.usecase.GetInitials
import ch.protonmail.android.mailcontact.presentation.model.ContactListItemUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactListItemUiModelMapper
import ch.protonmail.android.testdata.user.UserIdTestData
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.contact.domain.entity.ContactEmail
import me.proton.core.contact.domain.entity.ContactEmailId
import me.proton.core.contact.domain.entity.ContactId
import org.junit.Test
import kotlin.test.assertEquals

class ContactListItemUiModelMapperTest {

    private val getInitials = GetInitials()
    private val contactListItemUiModelMapper = ContactListItemUiModelMapper(getInitials)

    @Test
    fun `return correct contacts`() {
        val contacts = listOf(
            Contact(
                UserIdTestData.userId,
                ContactId("1"),
                "first contact",
                listOf(
                    ContactEmail(
                        UserIdTestData.userId,
                        ContactEmailId("contact email id 1"),
                        "First contact email",
                        "firstcontact+alias@protonmail.com",
                        0,
                        0,
                        ContactId("1"),
                        "firstcontact@protonmail.com",
                        emptyList(),
                        true,
                        lastUsedTime = 0
                    )
                )
            ),
            Contact(
                UserIdTestData.userId,
                ContactId("1.1"),
                "first contact bis",
                listOf(
                    ContactEmail(
                        UserIdTestData.userId,
                        ContactEmailId("contact email id 1.1"),
                        "First contact email bis",
                        "firstcontactbis@protonmail.com",
                        0,
                        1,
                        ContactId("1.1"),
                        "firstcontactbis@protonmail.com",
                        emptyList(),
                        true,
                        lastUsedTime = 0
                    )
                )
            ),
            Contact(
                UserIdTestData.userId,
                ContactId("2"),
                "second contact",
                listOf(
                    ContactEmail(
                        UserIdTestData.userId,
                        ContactEmailId("contact email id 2"),
                        "Second contact email",
                        "secondcontact@protonmail.com",
                        0,
                        0,
                        ContactId("2"),
                        "secondcontact@protonmail.com",
                        emptyList(),
                        true,
                        lastUsedTime = 0
                    )
                )
            )
        )

        val actual = contactListItemUiModelMapper.toContactListItemUiModel(contacts)

        val expected = listOf(
            ContactListItemUiModel.Header("F"),
            ContactListItemUiModel.Contact(
                id = ContactId("1"),
                name = "first contact",
                emailSubtext = TextUiModel("firstcontact+alias@protonmail.com"),
                avatar = AvatarUiModel.ParticipantInitial("FC")
            ),
            ContactListItemUiModel.Contact(
                id = ContactId("1.1"),
                name = "first contact bis",
                emailSubtext = TextUiModel("firstcontactbis@protonmail.com"),
                avatar = AvatarUiModel.ParticipantInitial("FB")
            ),
            ContactListItemUiModel.Header("S"),
            ContactListItemUiModel.Contact(
                id = ContactId("2"),
                name = "second contact",
                emailSubtext = TextUiModel("secondcontact@protonmail.com"),
                avatar = AvatarUiModel.ParticipantInitial("SC")
            )
        )

        assertEquals(actual, expected)
    }

    @Test
    fun `return contacts correctly sorted by name, with list headers`() {
        // Given
        val contacts = listOf(
            Contact(
                UserIdTestData.userId,
                ContactId("0"),
                "diego",
                emptyList()
            ),
            Contact(
                UserIdTestData.userId,
                ContactId("1"),
                "coccodrillo",
                emptyList()
            ),
            Contact(
                UserIdTestData.userId,
                ContactId("2"),
                "Clara",
                emptyList()
            ),
            Contact(
                UserIdTestData.userId,
                ContactId("3"),
                "abc",
                emptyList()
            ),
            Contact(
                UserIdTestData.userId,
                ContactId("4"),
                "adam",
                emptyList()
            ),
            Contact(
                UserIdTestData.userId,
                ContactId("5"),
                "Bella",
                emptyList()
            ),
            Contact(
                UserIdTestData.userId,
                ContactId("6"),
                " ",
                emptyList()
            ),
            Contact(
                UserIdTestData.userId,
                ContactId("7"),
                "",
                emptyList()
            )
        )

        // When
        val actual = contactListItemUiModelMapper.toContactListItemUiModel(contacts)

        // Then
        assertEquals((actual[0] as ContactListItemUiModel.Header).value, "?")
        assertEquals((actual[1] as ContactListItemUiModel.Contact).name, "")
        assertEquals((actual[2] as ContactListItemUiModel.Contact).name, "")
        assertEquals((actual[3] as ContactListItemUiModel.Header).value, "A")
        assertEquals((actual[4] as ContactListItemUiModel.Contact).name, "abc")
        assertEquals((actual[5] as ContactListItemUiModel.Contact).name, "adam")
        assertEquals((actual[6] as ContactListItemUiModel.Header).value, "B")
        assertEquals((actual[7] as ContactListItemUiModel.Contact).name, "Bella")
        assertEquals((actual[8] as ContactListItemUiModel.Header).value, "C")
        assertEquals((actual[9] as ContactListItemUiModel.Contact).name, "Clara")
        assertEquals((actual[10] as ContactListItemUiModel.Contact).name, "coccodrillo")
        assertEquals((actual[11] as ContactListItemUiModel.Header).value, "D")
        assertEquals((actual[12] as ContactListItemUiModel.Contact).name, "diego")
    }
}
