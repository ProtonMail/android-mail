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

import ch.protonmail.android.mailcommon.data.mapper.LocalContactSuggestion
import ch.protonmail.android.mailcontact.domain.model.ContactGroupId
import ch.protonmail.android.mailcontact.domain.model.ContactId
import ch.protonmail.android.mailcontact.domain.model.ContactMetadata
import uniffi.mail_uniffi.ContactSuggestionKind
import javax.inject.Inject

class ContactSuggestionsMapper @Inject constructor() {

    fun toContactSuggestions(localContacts: List<LocalContactSuggestion>): List<ContactMetadata> = localContacts.map {
        localContactSuggestionToContactMetadata(it)
    }

    private fun localContactSuggestionToContactMetadata(contact: LocalContactSuggestion) =
        when (val type = contact.kind) {
            is ContactSuggestionKind.DeviceContact -> {
                ContactMetadata.Contact(
                    id = contact.key.toContactId(),
                    name = contact.name,
                    emails = listOf(type.v1.toContactEmail()),
                    avatar = contact.avatarInformation.toAvatarInformation()
                )
            }

            is ContactSuggestionKind.ContactItem -> {
                ContactMetadata.Contact(
                    id = contact.key.toContactId(),
                    name = contact.name,
                    emails = listOf(type.v1.toContactEmail()),
                    avatar = contact.avatarInformation.toAvatarInformation()
                )
            }

            is ContactSuggestionKind.ContactGroup -> {
                ContactMetadata.ContactGroup(
                    id = contact.key.toContactGroupId(),
                    name = contact.name,
                    color = contact.avatarInformation.color,
                    members = type.v1.map { it.toContactEmail() }
                )
            }
        }

    private fun String.toContactId() = ContactId(this)

    private fun String.toContactGroupId() = ContactGroupId(this)
}


