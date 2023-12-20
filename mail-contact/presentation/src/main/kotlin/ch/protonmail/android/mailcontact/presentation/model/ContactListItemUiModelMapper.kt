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

package ch.protonmail.android.mailcontact.presentation.model

import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.presentation.R
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.contact.domain.entity.ContactEmail
import me.proton.core.util.kotlin.takeIfNotBlank

fun List<Contact>.toContactListItemUiModel(): List<ContactListItemUiModel> {
    val contacts = arrayListOf<ContactListItemUiModel>()
    this.map {
        it.copy(name = it.name.trim())
    }.sortedBy {
        it.name
    }.groupBy {
        it.name.first().uppercaseChar()
    }.forEach { nameGroup ->
        contacts.add(
            ContactListItemUiModel.Header(value = nameGroup.key.toString())
        )
        nameGroup.value.forEach { contact ->
            contacts.add(
                ContactListItemUiModel.Contact(
                    id = contact.id.id,
                    name = contact.name,
                    emailSubtext = getEmailSubtext(contact.contactEmails),
                    avatar = AvatarUiModel.ParticipantInitial(getInitials(contact.name))
                )
            )
        }
    }
    return contacts
}

private fun getEmailSubtext(contactEmails: List<ContactEmail>): TextUiModel {
    return if (contactEmails.isNotEmpty()) {
        val sortedContactEmails = contactEmails.sortedBy {
            it.order
        }.mapNotNull { contactEmail ->
            contactEmail.email.takeIfNotBlank()
        }
        if (sortedContactEmails.isEmpty()) {
            TextUiModel(R.string.no_contact_email)
        } else if (sortedContactEmails.size > 1) {
            TextUiModel(
                R.string.multiple_contact_emails,
                sortedContactEmails.first(),
                sortedContactEmails.size.minus(1)
            )
        } else {
            TextUiModel(sortedContactEmails.first())
        }
    } else {
        TextUiModel(R.string.no_contact_email)
    }
}

private fun getInitials(name: String, takeFirstOnly: Boolean? = false): String {
    if (name.isBlank()) return ""
    if (takeFirstOnly == true) return name.uppercase().take(1)
    val initials = name.uppercase().split(' ')
        .mapNotNull { it.firstOrNull()?.toString() }
        .reduce { acc, s -> acc + s }
    // Keep only the first and last initials
    return if (initials.length > 2) initials[0].toString() + initials[initials.lastIndex] else initials
}
