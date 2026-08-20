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

package ch.protonmail.android.mailcontact.data.mapper

import ch.protonmail.android.mailcommon.domain.model.AvatarInformation
import ch.protonmail.android.mailcontact.domain.model.ContactEmail
import ch.protonmail.android.mailcontact.domain.model.ContactId
import ch.protonmail.android.mailcontact.domain.model.ContactMetadata
import ch.protonmail.android.testdata.contact.rust.LocalContactTestData
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactItemMapperTest {

    private val contactItemMapper = ContactItemMapper()

    @Test
    fun `should map local contact to domain model correctly`() {
        // Given
        val localContact = LocalContactTestData.contact1
        val expectedContact = ContactMetadata.Contact(
            id = localContact.v1.id.toContactId(),
            name = localContact.v1.name,
            emails = listOf(
                ContactEmail(
                    id = ContactId("101"),
                    email = "alice.johnson@example.com",
                    isProton = false,
                    lastUsedTime = 0,
                    name = localContact.v1.name,
                    avatarInformation = localContact.v1.avatarInformation.toAvatarInformation()
                ),
                ContactEmail(
                    id = ContactId("102"),
                    email = "alice.work@example.com",
                    isProton = false,
                    lastUsedTime = 0,
                    name = localContact.v1.name,
                    avatarInformation = localContact.v1.avatarInformation.toAvatarInformation()
                )
            ),
            avatar = AvatarInformation(
                initials = "A",
                color = "#FF5733"
            )
        )

        // When
        val result = contactItemMapper.toContact(localContact)

        // Then
        assertEquals(expectedContact, result)
    }

    @Test
    fun `should map device contact to domain model correctly`() {
        // Given
        val deviceContact = LocalContactTestData.deviceContact1
        val expectedAvatar = AvatarInformation(initials = "CD", color = "#1BA3A3")
        val expectedContact = ContactMetadata.Contact(
            id = ContactId("device-contact-1"),
            name = deviceContact.v1.name,
            emails = listOf(
                ContactEmail(
                    id = ContactId("device-contact-1"),
                    email = "example1@example.com",
                    isProton = false,
                    lastUsedTime = 0,
                    name = deviceContact.v1.name,
                    avatarInformation = expectedAvatar
                ),
                ContactEmail(
                    id = ContactId("device-contact-1"),
                    email = "example2@example.com",
                    isProton = false,
                    lastUsedTime = 0,
                    name = deviceContact.v1.name,
                    avatarInformation = expectedAvatar
                )
            ),
            avatar = expectedAvatar
        )

        // When
        val result = contactItemMapper.toContact(deviceContact)

        // Then
        assertEquals(expectedContact, result)
    }

    @Test
    fun `should map device contact with no emails to domain model with empty emails`() {
        // Given
        val deviceContact = LocalContactTestData.deviceContactNoEmails
        val expectedContact = ContactMetadata.Contact(
            id = ContactId("device-contact-2"),
            name = deviceContact.v1.name,
            emails = emptyList(),
            avatar = AvatarInformation(initials = "DD", color = "#A31B1B")
        )

        // When
        val result = contactItemMapper.toContact(deviceContact)

        // Then
        assertEquals(expectedContact, result)
    }

}
