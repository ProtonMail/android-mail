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

import ch.protonmail.android.mailcommon.data.mapper.LocalContactItemTypeContact
import ch.protonmail.android.mailcommon.data.mapper.LocalContactItemTypeDevice
import ch.protonmail.android.mailcommon.domain.model.AvatarInformation
import ch.protonmail.android.mailcontact.domain.model.ContactEmail
import ch.protonmail.android.mailcontact.domain.model.ContactId
import ch.protonmail.android.mailcontact.domain.model.ContactMetadata
import javax.inject.Inject

class ContactItemMapper @Inject constructor() {
    fun toContact(localContactItem: LocalContactItemTypeContact): ContactMetadata.Contact {
        return ContactMetadata.Contact(
            id = localContactItem.v1.id.toContactId(),
            name = localContactItem.v1.name,
            emails = localContactItem.v1.emails.map { localEmail ->
                localEmail.toContactEmail()
            },
            avatar = AvatarInformation(
                initials = localContactItem.v1.avatarInformation.text,
                color = localContactItem.v1.avatarInformation.color
            )
        )
    }

    /**
     * This is not being consumed at all by the clients, it's just in place to provide compatibility.
     */
    fun toContact(deviceContactItem: LocalContactItemTypeDevice): ContactMetadata.Contact {
        val contactId = ContactId(deviceContactItem.v1.id)
        val avatarInformation = AvatarInformation(
            initials = deviceContactItem.v1.avatarInformation.text,
            color = deviceContactItem.v1.avatarInformation.color
        )

        return ContactMetadata.Contact(
            id = contactId,
            name = deviceContactItem.v1.name,
            emails = deviceContactItem.v1.emails.map { localEmail ->
                ContactEmail(
                    id = contactId,
                    name = deviceContactItem.v1.name,
                    email = localEmail,
                    isProton = false, // Not exposed by the SDK
                    lastUsedTime = 0L, // Not exposed by the SDK
                    avatarInformation = avatarInformation
                )
            },
            avatar = avatarInformation
        )
    }
}
