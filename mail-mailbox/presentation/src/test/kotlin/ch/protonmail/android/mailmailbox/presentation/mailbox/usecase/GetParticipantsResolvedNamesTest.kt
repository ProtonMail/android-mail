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

package ch.protonmail.android.mailmailbox.presentation.mailbox.usecase

import ch.protonmail.android.mailconversation.domain.entity.Recipient
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.testdata.contact.ContactTestData
import ch.protonmail.android.testdata.mailbox.MailboxTestData.buildMailboxItem
import ch.protonmail.android.testdata.user.UserIdTestData
import org.junit.Test
import kotlin.test.assertEquals

class GetParticipantsResolvedNamesTest {

    private val useCase = GetParticipantsResolvedNames()

    @Test
    fun `when mailbox item is not in sent or drafts ui model shows senders names as participants`() {
        // Given
        val senders = listOf(
            Recipient("sender@proton.ch", "sender"),
            Recipient("sender1@proton.ch", "sender1"),
        )
        val mailboxItem = buildMailboxItem(
            labelIds = listOf(SystemLabelId.Inbox.labelId),
            senders = senders
        )
        // When
        val actual = useCase(mailboxItem, ContactTestData.contacts)
        // Then
        val expected = listOf("sender", "sender1")
        assertEquals(expected, actual)
    }

    @Test
    fun `when mailbox item is in sent or drafts ui model shows recipients names as participants`() {
        // Given
        val recipients = listOf(
            Recipient("recipient@proton.ch", "recipient"),
            Recipient("recipient1@proton.ch", "recipient1"),
        )
        val mailboxItem = buildMailboxItem(
            labelIds = listOf(SystemLabelId.Sent.labelId),
            recipients = recipients
        )
        // When
        val actual = useCase(mailboxItem, ContactTestData.contacts)
        // Then
        val expected = listOf("recipient", "recipient1")
        assertEquals(expected, actual)
    }

    @Test
    fun `when any participant exists as contact then contact name is mapped to the ui model`() {
        // Given
        val contact = ContactTestData.buildContactWith(
            userId = UserIdTestData.userId,
            contactEmails = listOf(
                ContactTestData.buildContactEmailWith(
                    name = "contact email name",
                    address = "sender1@proton.ch"
                )
            )
        )
        val userContacts = listOf(contact, ContactTestData.contact2)
        val senders = listOf(
            Recipient("sender@proton.ch", "sender"),
            Recipient("sender1@proton.ch", ""),
        )
        val mailboxItem = buildMailboxItem(
            labelIds = listOf(SystemLabelId.Inbox.labelId),
            senders = senders
        )
        // When
        val actual = useCase(mailboxItem, userContacts)
        // Then
        val expected = listOf("sender", "contact email name")
        assertEquals(expected, actual)
    }

    @Test
    fun `when any participant has no display name defined address is mapped to the ui model`() {
        // Given
        val senders = listOf(
            Recipient("sender@proton.ch", "sender"),
            Recipient("sender1@proton.ch", ""),
        )
        val mailboxItem = buildMailboxItem(
            labelIds = listOf(SystemLabelId.Inbox.labelId),
            senders = senders
        )
        // When
        val actual = useCase(mailboxItem, ContactTestData.contacts)
        // Then
        val expected = listOf("sender", "sender1@proton.ch")
        assertEquals(expected, actual)
    }

}