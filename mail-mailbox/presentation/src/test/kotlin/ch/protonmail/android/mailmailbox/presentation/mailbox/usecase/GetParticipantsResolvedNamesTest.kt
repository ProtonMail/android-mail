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
import ch.protonmail.android.mailmailbox.domain.usecase.ParticipantsResolvedNamesResult
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.model.Sender
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantNameResult
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
        val sender1Name = "sender1"
        val sender2Name = "sender2"
        val resolveParticipant1NameResult = ResolveParticipantNameResult(sender1Name, isProton = true)
        val resolveParticipant2NameResult = ResolveParticipantNameResult(sender2Name, isProton = false)
        val sender = Sender("sender@proton.ch", sender1Name)
        val sender1 = Sender("sender1@proton.ch", sender2Name)
        val senders = listOf(sender, sender1)
        val mailboxItem = buildMailboxItem(
            labelIds = listOf(SystemLabelId.Inbox.labelId),
            senders = senders
        )
        every { resolveParticipantName(sender, ContactTestData.contacts) } returns resolveParticipant1NameResult
        every { resolveParticipantName(sender1, ContactTestData.contacts) } returns resolveParticipant2NameResult
        // When
        val actual = useCase(mailboxItem, ContactTestData.contacts)
        // Then
        val expected = ParticipantsResolvedNamesResult.Senders(
            listOf(
                resolveParticipant1NameResult,
                resolveParticipant2NameResult
            )
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `when message is in all sent or all drafts ui model shows recipients names as participants`() {
        // Given
        val recipient1Name = "recipient"
        val recipient2Name = "recipient1"
        val resolveParticipant1NameResult = ResolveParticipantNameResult(recipient1Name, isProton = false)
        val resolveParticipant2NameResult = ResolveParticipantNameResult(recipient2Name, isProton = true)
        val recipient = Recipient("recipient@proton.ch", recipient1Name)
        val recipient1 = Recipient("recipient1@proton.ch", recipient2Name)
        val recipients = listOf(recipient, recipient1)
        val mailboxItem = buildMailboxItem(
            type = MailboxItemType.Message,
            labelIds = listOf(SystemLabelId.AllSent.labelId),
            recipients = recipients
        )
        every { resolveParticipantName(recipient, ContactTestData.contacts) } returns resolveParticipant1NameResult
        every { resolveParticipantName(recipient1, ContactTestData.contacts) } returns resolveParticipant2NameResult
        // When
        val actual = useCase(mailboxItem, ContactTestData.contacts)
        // Then
        val expected = ParticipantsResolvedNamesResult.Recipients(
            listOf(
                resolveParticipant1NameResult,
                resolveParticipant2NameResult
            )
        )
        assertEquals(expected, actual)
    }
}
