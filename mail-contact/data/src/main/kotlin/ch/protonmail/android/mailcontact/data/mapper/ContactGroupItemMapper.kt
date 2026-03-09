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

import ch.protonmail.android.mailcommon.data.mapper.LocalContactItemTypeGroup
import ch.protonmail.android.mailcontact.domain.model.ContactMetadata
import uniffi.mail_uniffi.ContactGroupItem
import javax.inject.Inject

class ContactGroupItemMapper @Inject constructor() {

    fun toContactGroup(localContactGroupItem: LocalContactItemTypeGroup): ContactMetadata.ContactGroup {
        return ContactMetadata.ContactGroup(
            id = localContactGroupItem.v1.id.toContactGroupId(),
            name = localContactGroupItem.v1.name,
            color = localContactGroupItem.v1.avatarColor,
            members = localContactGroupItem.v1.contactEmails.map { it.toContactEmail() }
        )
    }

    fun toContactGroup(contactGroupItem: ContactGroupItem): ContactMetadata.ContactGroup {
        return ContactMetadata.ContactGroup(
            id = contactGroupItem.id.toContactGroupId(),
            name = contactGroupItem.name,
            color = contactGroupItem.avatarColor,
            members = contactGroupItem.contactEmails.map { it.toContactEmail() }
        )
    }
}
