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

import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.usecase.GetParticipantsResolvedNames
import ch.protonmail.android.mailmessage.domain.entity.Recipient
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import ch.protonmail.android.testdata.contact.ContactTestData
import ch.protonmail.android.testdata.mailbox.MailboxTestData.buildMailboxItem
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

class GetParticipantsResolvedNamesTest {

    private val resolveParticipantName = mockk<ResolveParticipantName>()
    private val useCase = GetParticipantsResolvedNames(resolveParticipantName)

    @Test
    fun `when mailbox item is not in all sent or all drafts ui model shows senders names as participants`() {
        // Given
        val senderName = "sender"
        val sender1Name = "sender1"
        val sender = Recipient("sender@proton.ch", senderName)
        val sender1 = Recipient("sender1@proton.ch", sender1Name)
        val senders = listOf(sender, sender1)
        val mailboxItem = buildMailboxItem(
            labelIds = listOf(SystemLabelId.Inbox.labelId),
            senders = senders
        )
        every { resolveParticipantName(sender, ContactTestData.contacts) } returns senderName
        every { resolveParticipantName(sender1, ContactTestData.contacts) } returns sender1Name
        // When
        val actual = useCase(mailboxItem, ContactTestData.contacts)
        // Then
        val expected = listOf(senderName, sender1Name)
        assertEquals(expected, actual)
    }

    @Test
    fun `when message is in all sent or all drafts ui model shows recipients names as participants`() {
        // Given
        val recipientName = "recipient"
        val recipient1Name = "recipient1"
        val recipient = Recipient("recipient@proton.ch", recipientName)
        val recipient1 = Recipient("recipient1@proton.ch", recipient1Name)
        val recipients = listOf(recipient, recipient1)
        val mailboxItem = buildMailboxItem(
            type = MailboxItemType.Message,
            labelIds = listOf(SystemLabelId.AllSent.labelId),
            recipients = recipients
        )
        every { resolveParticipantName(recipient, ContactTestData.contacts) } returns recipientName
        every { resolveParticipantName(recipient1, ContactTestData.contacts) } returns recipient1Name
        // When
        val actual = useCase(mailboxItem, ContactTestData.contacts)
        // Then
        val expected = listOf(recipientName, recipient1Name)
        assertEquals(expected, actual)
    }
}
