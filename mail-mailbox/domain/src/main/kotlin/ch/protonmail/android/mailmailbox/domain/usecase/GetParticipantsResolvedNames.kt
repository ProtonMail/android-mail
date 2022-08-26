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

package ch.protonmail.android.mailmailbox.domain.usecase

import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmessage.domain.entity.Recipient
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject

class GetParticipantsResolvedNames @Inject constructor() {

    operator fun invoke(mailboxItem: MailboxItem, contacts: List<Contact>): List<String> {
        val displayRecipientLocations = setOf(
            SystemLabelId.AllSent.labelId,
            SystemLabelId.AllDrafts.labelId
        )
        val shouldDisplayRecipients = mailboxItem.labelIds.any { it in displayRecipientLocations }

        return if (shouldDisplayRecipients && mailboxItem.type == MailboxItemType.Message) {
            mailboxItem.recipients.map { getPreferredName(contacts, it) }
        } else {
            mailboxItem.senders.map { getPreferredName(contacts, it) }
        }
    }

    private fun getPreferredName(contacts: List<Contact>, recipient: Recipient): String {
        val contactEmail = contacts.firstNotNullOfOrNull { contact ->
            contact.contactEmails.find { it.email == recipient.address }
        }

        return contactEmail?.name?.takeIfNotBlank() ?: recipient.name.ifEmpty { recipient.address }
    }

}
