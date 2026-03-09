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

package ch.protonmail.android.testdata.contact.rust

import ch.protonmail.android.mailcommon.data.mapper.LocalContactGroupId
import ch.protonmail.android.mailcommon.data.mapper.LocalContactId
import ch.protonmail.android.mailcommon.data.mapper.LocalContactItemTypeContact
import ch.protonmail.android.mailcommon.data.mapper.LocalContactItemTypeGroup
import ch.protonmail.android.mailcommon.data.mapper.LocalGroupedContacts
import uniffi.mail_uniffi.AvatarInformation
import uniffi.mail_uniffi.ContactEmailItem
import uniffi.mail_uniffi.ContactGroupItem
import uniffi.mail_uniffi.ContactItem
import uniffi.mail_uniffi.Id

object LocalContactTestData {

    val contactId1 = LocalContactId(100u)
    val contactId2 = LocalContactId(103u)
    val contactGroupId1 = LocalContactGroupId(105u)
    val contactGroupId2 = LocalContactGroupId(107u)
    private val avatar1 = AvatarInformation(
        text = "A",
        color = "#FF5733"
    )
    private const val NAME1 = "Alice Johnson"
    val contact1 = LocalContactItemTypeContact(
        ContactItem(
            id = contactId1,
            name = NAME1,
            avatarInformation = avatar1,
            emails = listOf(
                ContactEmailItem(
                    contactId = Id(101u),
                    email = "alice.johnson@example.com",
                    isProton = false,
                    lastUsedTime = 0uL,
                    name = NAME1,
                    avatarInformation = avatar1
                ),
                ContactEmailItem(
                    contactId = Id(102u),
                    email = "alice.work@example.com",
                    isProton = false,
                    lastUsedTime = 0uL,
                    name = NAME1,
                    avatarInformation = avatar1
                )
            )
        )
    )

    private val avatar2 = AvatarInformation(
        text = "B",
        color = "#33AFFF"
    )
    private const val NAME2 = "Bob Smith"
    val contact2 = LocalContactItemTypeContact(
        ContactItem(
            id = contactId2,
            name = NAME2,
            avatarInformation = avatar2,
            emails = listOf(
                ContactEmailItem(
                    contactId = Id(104u),
                    email = "bob.smith@example.com",
                    isProton = false,
                    lastUsedTime = 0uL,
                    name = NAME2,
                    avatarInformation = avatar2
                )
            )
        )
    )

    val contactGroup1 = LocalContactItemTypeGroup(
        ContactGroupItem(
            id = contactGroupId1,
            name = "A Family",
            avatarColor = "#FFD700",
            contactEmails = listOf(
                ContactEmailItem(
                    contactId = Id(1u),
                    "family@example.com",
                    isProton = false,
                    lastUsedTime = 0uL,
                    name = "Family",
                    avatarInformation = AvatarInformation("Fam", "#FFD700")
                )
            )
        )
    )

    val contactGroup2 = LocalContactItemTypeGroup(
        ContactGroupItem(
            id = contactGroupId2,
            name = "B Work Colleagues",
            avatarColor = "#8A2BE2",
            contactEmails = listOf(
                ContactEmailItem(
                    contactId = Id(1u),
                    email = "work@example.com",
                    isProton = false,
                    lastUsedTime = 0uL,
                    name = "Work",
                    avatarInformation = AvatarInformation("Wk", "#8A2BE2")
                ),
                ContactEmailItem(
                    contactId = Id(1u),
                    email = "team@example.com",
                    isProton = false,
                    lastUsedTime = 0uL,
                    name = "Team",
                    avatarInformation = AvatarInformation("Tm", "#8A2BE2")
                )
            )
        )
    )

    val groupedContactsByA = LocalGroupedContacts(
        groupedBy = "A",
        items = listOf(contact1, contactGroup1)
    )

    val groupedContactsByB = LocalGroupedContacts(
        groupedBy = "B",
        items = listOf(contact2, contactGroup2)
    )
}
