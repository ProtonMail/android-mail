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

package ch.protonmail.android.mailcomposer.presentation.usecase

import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionUiModel
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.mailcontact.domain.model.DeviceContact
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject

class SortContactsForSuggestions @Inject constructor() {

    operator fun invoke(
        contacts: List<Contact>,
        deviceContacts: List<DeviceContact>,
        contactGroups: List<ContactGroup>,
        maxContactAutocompletionCount: Int
    ): List<ContactSuggestionUiModel> {

        val fromContacts = contacts.asSequence().flatMap { contact ->
            contact.contactEmails.map {
                contact.copy(
                    contactEmails = listOf(it)
                ) // flatMap into Contacts containing only one ContactEmail because we need to sort by them
            }
        }.sortedBy {
            val lastUsedTimeDescending = Long.MAX_VALUE - it.contactEmails.first().lastUsedTime

            // LastUsedTime, name, email
            "$lastUsedTimeDescending ${it.name} ${it.contactEmails.first().email ?: ""}"
        }.map { contact ->
            val contactEmail = contact.contactEmails.first()
            ContactSuggestionUiModel.Contact(
                name = contactEmail.name.takeIfNotBlank()
                    ?: contact.name.takeIfNotBlank()
                    ?: contactEmail.email,
                email = contactEmail.email
            )
        }

        val fromDeviceContacts = deviceContacts.asSequence().map {
            ContactSuggestionUiModel.Contact(
                name = it.name,
                email = it.email
            )
        }

        val fromContactGroups = contactGroups.asSequence().map { contactGroup ->
            ContactSuggestionUiModel.ContactGroup(
                name = contactGroup.name,
                emails = contactGroup.members.map { it.email }
            )
        }

        val fromDeviceAndContactGroups = (fromDeviceContacts + fromContactGroups).sortedBy {
            it.name
        }

        return (fromContacts + fromDeviceAndContactGroups)
            .take(maxContactAutocompletionCount)
            .toList()

    }

}
