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

    private val contactListItemUiModelMapper = ContactListItemUiModelMapper()

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
                        true
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
                        true
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
                        true
                    )
                )
            )
        )

        val actual = contactListItemUiModelMapper.toContactListItemUiModel(contacts)

        val expected = listOf(
            ContactListItemUiModel.Header("F"),
            ContactListItemUiModel.Contact(
                id = "1",
                name = "first contact",
                emailSubtext = TextUiModel("firstcontact+alias@protonmail.com"),
                avatar = AvatarUiModel.ParticipantInitial("FC")
            ),
            ContactListItemUiModel.Contact(
                id = "1.1",
                name = "first contact bis",
                emailSubtext = TextUiModel("firstcontactbis@protonmail.com"),
                avatar = AvatarUiModel.ParticipantInitial("FB")
            ),
            ContactListItemUiModel.Header("S"),
            ContactListItemUiModel.Contact(
                id = "2",
                name = "second contact",
                emailSubtext = TextUiModel("secondcontact@protonmail.com"),
                avatar = AvatarUiModel.ParticipantInitial("SC")
            )
        )

        assertEquals(actual, expected)
    }
}
