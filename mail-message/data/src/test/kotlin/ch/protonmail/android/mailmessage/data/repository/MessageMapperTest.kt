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

package ch.protonmail.android.mailmessage.data.repository

import ch.protonmail.android.mailattachments.data.mapper.toAttachmentId
import ch.protonmail.android.mailattachments.data.mapper.toAttachmentMetadata
import ch.protonmail.android.mailattachments.data.mapper.toMimeTypeCategory
import ch.protonmail.android.mailcommon.data.mapper.LocalAddressId
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentId
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentMetadata
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentMimeType
import ch.protonmail.android.mailcommon.data.mapper.LocalAvatarInformation
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.mapper.LocalExclusiveLocationSystem
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageId
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageMetadata
import ch.protonmail.android.mailcommon.data.mapper.LocalMimeTypeCategory
import ch.protonmail.android.maillabel.data.mapper.toExclusiveLocation
import ch.protonmail.android.maillabel.data.mapper.toLabel
import ch.protonmail.android.mailmessage.data.mapper.toAddressId
import ch.protonmail.android.mailmessage.data.mapper.toConversationId
import ch.protonmail.android.mailmessage.data.mapper.toMessage
import ch.protonmail.android.mailmessage.data.mapper.toParticipant
import ch.protonmail.android.mailmessage.data.mapper.toRecipient
import ch.protonmail.android.mailmessage.domain.model.MessageId
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import uniffi.mail_uniffi.AvatarInformation
import uniffi.mail_uniffi.Disposition
import uniffi.mail_uniffi.InlineCustomLabel
import uniffi.mail_uniffi.LabelColor
import uniffi.mail_uniffi.MessageFlags
import uniffi.mail_uniffi.MessageRecipient
import uniffi.mail_uniffi.MessageSender
import uniffi.mail_uniffi.SystemLabel

class MessageMapperTest {

    @Test
    fun `message sender to participant should convert correctly`() {
        // Given
        val address = "address@test.com"
        val name = "Name"
        val isProton = true
        val displaySenderImage = false
        val isSimpleLogin = false
        val bimiSelector = "bimiSelector"

        val messageAddress = MessageSender(
            address = address,
            name = name,
            isProton = isProton,
            displaySenderImage = displaySenderImage,
            isSimpleLogin = isSimpleLogin,
            bimiSelector = bimiSelector
        )

        // When
        val participant = messageAddress.toParticipant()

        // Then
        assertEquals(address, participant.address)
        assertEquals(name, participant.name)
        assertTrue(participant.isProton)
        assertEquals(bimiSelector, participant.bimiSelector)
    }


    @Test
    fun `message recipient to recipient should convert correctly`() {
        // Given
        val address = "recipient@test.com"
        val name = "Recipient Name"
        val isProton = true

        val messageAddress = MessageRecipient(
            address = address,
            name = name,
            isProton = isProton,
            group = null
        )

        // When
        val recipient = messageAddress.toRecipient()

        // Then
        assertEquals(address, recipient.address)
        assertEquals(name, recipient.name)
        assertTrue(recipient.isProton)
    }

    @Test
    fun `LocalAttachmentMetadata toAttachmentMetadata should convert correctly`() {
        // Given
        val localAttachmentId = LocalAttachmentId(1uL)
        val name = "Test Attachment"
        val size = 1024uL
        val mimeType = LocalAttachmentMimeType("application/pdf", LocalMimeTypeCategory.PDF)

        val localAttachmentMetadata = LocalAttachmentMetadata(
            id = localAttachmentId,
            name = name,
            size = size,
            mimeType = mimeType,
            disposition = Disposition.ATTACHMENT,
            isListable = true
        )

        // When
        val attachmentMetadata = localAttachmentMetadata.toAttachmentMetadata()

        // Then
        assertEquals(localAttachmentId.toAttachmentId(), attachmentMetadata.attachmentId)
        assertEquals(name, attachmentMetadata.name)
        assertEquals(size.toLong(), attachmentMetadata.size)
        assertEquals(mimeType.category.toMimeTypeCategory(), attachmentMetadata.mimeType.category)
    }

    @Test
    fun `LocalMessageMetadata toMessage should convert correctly`() {
        // Given
        val id = 111uL
        val conversationId = LocalConversationId(99uL)
        val time = 1625234000000uL
        val snoozeTime = 1625888000000uL
        val size = 1024uL
        val order = 1uL
        val labels = listOf(
            InlineCustomLabel(id = LocalLabelId(1u), name = "Test Label", color = LabelColor("0xFF0000"))
        )
        val subject = "Test Subject"
        val unread = true
        val sender = MessageSender(
            "sender@test.com", "Sender", true,
            false, false, "bimiSelector"
        )
        val to = listOf(
            MessageRecipient(
                "to1@test.com", true, "To1", null
            ),
            MessageRecipient(
                "to2@test.com", false, "To2", null
            )
        )
        val cc = listOf(
            MessageRecipient(
                "cc1@test.com", true, "Cc1", null
            ),
            MessageRecipient(
                "cc2@test.com", false, "Cc2", null
            )
        )
        val bcc = listOf(
            MessageRecipient(
                "bcc1@test.com", true, "Bcc1", null
            ),
            MessageRecipient(
                "bcc2@test.com", false, "Bcc2", null
            )
        )
        val expirationTime = 1625235000000u
        val isReplied = false
        val isRepliedAll = false
        val isForwarded = false
        val addressId = LocalAddressId(1.toULong())
        val numAttachments = 0u
        val flags = MessageFlags(1897uL)
        val starred = false
        val attachments: List<LocalAttachmentMetadata> = emptyList()
        val avatarInformation = AvatarInformation("A", "blue")
        val exclusiveLocation = LocalExclusiveLocationSystem(
            name = SystemLabel.SENT,
            id = LocalLabelId(100u)
        )

        val localMessageMetadata = LocalMessageMetadata(
            id = LocalMessageId(id),
            conversationId = conversationId,
            time = time,
            size = size,
            displayOrder = order,
            customLabels = labels,
            subject = subject,
            unread = unread,
            sender = sender,
            toList = to,
            ccList = cc,
            bccList = bcc,
            expirationTime = expirationTime,
            snoozedUntil = snoozeTime,
            isReplied = isReplied,
            isRepliedAll = isRepliedAll,
            isForwarded = isForwarded,
            addressId = addressId,
            numAttachments = numAttachments,
            flags = flags,
            starred = starred,
            attachmentsMetadata = attachments,
            location = exclusiveLocation,
            avatar = avatarInformation,
            isDraft = false,
            isScheduled = false,
            canReply = false,
            displaySnoozeReminder = false
        )

        // When
        val message = localMessageMetadata.toMessage()

        // Then
        assertEquals(MessageId(id.toString()), message.messageId)
        assertEquals(conversationId.toConversationId(), message.conversationId)
        assertEquals(time.toLong(), message.time)
        assertEquals(size.toLong(), message.size)
        assertEquals(order.toLong(), message.order)
        assertEquals(labels.map { it.toLabel() }, message.customLabels)
        assertEquals(subject, message.subject)
        assertTrue(message.isUnread)
        assertEquals(sender.toParticipant(), message.sender)
        assertEquals(to.map { it.toRecipient() }, message.toList)
        assertEquals(cc.map { it.toRecipient() }, message.ccList)
        assertEquals(bcc.map { it.toRecipient() }, message.bccList)
        assertEquals(expirationTime.toLong(), message.expirationTime)
        assertFalse(message.isReplied)
        assertFalse(message.isRepliedAll)
        assertFalse(message.isForwarded)
        assertEquals(addressId.toAddressId(), message.addressId)
        assertEquals(numAttachments.toInt(), message.numAttachments)
        assertEquals(flags.value.toLong(), message.flags)
        assertEquals(0, message.attachmentCount.calendar)
        assertEquals(exclusiveLocation.toExclusiveLocation(), message.exclusiveLocation)
    }

    @Test
    fun `LocalMessageMetadata toMessage should include attachments correctly`() {
        // Given
        val localAttachmentId1 = LocalAttachmentId(1uL)
        val localAttachmentId2 = LocalAttachmentId(2uL)
        val attachment1 = LocalAttachmentMetadata(
            id = localAttachmentId1,
            name = "Attachment1.pdf",
            size = 2048uL,
            mimeType = LocalAttachmentMimeType("application/pdf", LocalMimeTypeCategory.PDF),
            disposition = Disposition.ATTACHMENT,
            isListable = true
        )
        val attachment2 = LocalAttachmentMetadata(
            id = localAttachmentId2,
            name = "Attachment2.txt",
            size = 1024uL,
            mimeType = LocalAttachmentMimeType("plain/text", LocalMimeTypeCategory.TEXT),
            disposition = Disposition.ATTACHMENT,
            isListable = true
        )
        val localMessageMetadata = LocalMessageMetadata(
            id = LocalMessageId(1uL),
            conversationId = LocalConversationId(99uL),
            time = 1625234000000uL,
            size = 4096uL,
            displayOrder = 1uL,
            customLabels = emptyList(),
            subject = "Test Subject",
            unread = true,
            sender = MessageSender(
                address = "sender@test.com",
                name = "Sender",
                isProton = true,
                displaySenderImage = false,
                isSimpleLogin = false,
                bimiSelector = "bimiSelector"
            ),
            toList = emptyList(),
            ccList = emptyList(),
            bccList = emptyList(),
            expirationTime = 1625235000000u,
            snoozedUntil = 1625888000000uL,
            isReplied = false,
            isRepliedAll = false,
            isForwarded = false,
            addressId = LocalAddressId(1uL),
            numAttachments = 2u,
            flags = MessageFlags(1897uL),
            starred = false,
            attachmentsMetadata = listOf(attachment1, attachment2),
            location = null,
            avatar = LocalAvatarInformation("S", "blue"),
            isDraft = false,
            isScheduled = false,
            canReply = false,
            displaySnoozeReminder = false
        )

        // When
        val message = localMessageMetadata.toMessage()

        // Then
        assertEquals(2, message.attachmentPreviews.size)
        assertEquals(0, message.attachmentCount.calendar)
        assertEquals(attachment1.toAttachmentMetadata(), message.attachmentPreviews[0])
        assertEquals(attachment2.toAttachmentMetadata(), message.attachmentPreviews[1])
    }
}

