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

package ch.protonmail.android.mailconversation.data.mapper

import ch.protonmail.android.mailattachments.domain.model.MimeTypeCategory
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentMetadata
import ch.protonmail.android.mailcommon.data.mapper.LocalConversation
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.mapper.LocalExclusiveLocationSystem
import ch.protonmail.android.mailcommon.data.mapper.LocalHiddenMessagesBanner
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.maillabel.data.mapper.toExclusiveLocation
import ch.protonmail.android.maillabel.data.mapper.toLabel
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.data.mapper.toParticipant
import ch.protonmail.android.mailmessage.data.sample.LocalAttachmentMetadataSample
import ch.protonmail.android.mailsnooze.domain.model.NoSnooze
import ch.protonmail.android.mailsnooze.domain.model.SnoozeReminder
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import uniffi.mail_uniffi.AvatarInformation
import uniffi.mail_uniffi.HiddenMessagesBanner
import uniffi.mail_uniffi.InlineCustomLabel
import uniffi.mail_uniffi.LabelColor
import uniffi.mail_uniffi.MessageRecipient
import uniffi.mail_uniffi.MessageSender
import uniffi.mail_uniffi.SystemLabel
import kotlin.test.assertEquals

class ConversationMapperTest {

    @Test
    fun `local conversation without attachments to conversation should convert correctly`() {
        // Given
        val localConversation = createLocalConversation()

        // When
        val conversation = localConversation.toConversation()

        // Then
        val expectedId = ConversationId(localConversation.id.value.toString())
        assertEquals(expectedId, conversation.conversationId)
        assertEquals(localConversation.displayOrder.toLong(), conversation.order)
        assertEquals(localConversation.subject, conversation.subject)
        assertEquals(localConversation.senders.map { it.toParticipant() }, conversation.senders)
        assertEquals(localConversation.recipients.map { it.toParticipant() }, conversation.recipients)
        assertEquals(localConversation.totalMessages.toInt(), conversation.numMessages)
        assertEquals(localConversation.numUnread.toInt(), conversation.numUnread)
        assertEquals(localConversation.numAttachments.toInt(), conversation.numAttachments)
        assertEquals(localConversation.expirationTime.toLong(), conversation.expirationTime)
        assertEquals(localConversation.avatar.text, conversation.avatarInformation.initials)
        assertEquals(localConversation.avatar.color, conversation.avatarInformation.color)
        assertEquals(
            localConversation.customLabels.map { it.toLabel() },
            conversation.customLabels
        )
        assertEquals(0, conversation.attachmentCount.calendar)
        assertFalse(conversation.isStarred)
        assertEquals(localConversation.time.toLong(), conversation.time)
        assertEquals(localConversation.size.toLong(), conversation.size)
        assertEquals(localConversation.locations.map { it.toExclusiveLocation() }, conversation.exclusiveLocation)
        assertEquals(NoSnooze, conversation.snoozeInformation)
    }


    @Test
    fun `Local conversation with non-calendar attachments to conversation should convert correctly`() {
        // Given
        val attachments = listOf(
            LocalAttachmentMetadataSample.Pdf,
            LocalAttachmentMetadataSample.Image,
            LocalAttachmentMetadataSample.Text
        )
        val localConversation = createLocalConversation(
            attachmentsMetadata = attachments
        )

        // When
        val conversation = localConversation.toConversation()

        // Then
        assertEquals(attachments.size, conversation.attachments.size)
        assertEquals(0, conversation.attachmentCount.calendar)
        assertTrue(conversation.attachments.all { it.mimeType.category != MimeTypeCategory.Calendar })
    }

    @Test
    fun `Local conversation with one calendar type attachment to conversation should convert correctly`() {
        // Given
        val attachments = listOf(LocalAttachmentMetadataSample.Calendar)
        val localConversation = createLocalConversation(attachmentsMetadata = attachments)

        // When
        val conversation = localConversation.toConversation()

        // Then
        assertEquals(1, conversation.attachments.size)
        assertEquals(1, conversation.attachmentCount.calendar)
        assertEquals(MimeTypeCategory.Calendar, conversation.attachments[0].mimeType.category)
    }

    @Test
    fun `Local conversation with a calendar and several other attachment types should convert correctly`() {
        // Given
        val attachments = listOf(
            LocalAttachmentMetadataSample.Calendar,
            LocalAttachmentMetadataSample.Pdf,
            LocalAttachmentMetadataSample.Image
        )
        val localConversation = createLocalConversation(attachmentsMetadata = attachments)

        // When
        val conversation = localConversation.toConversation()

        // Then
        assertEquals(attachments.size, conversation.attachments.size)
        assertEquals(1, conversation.attachmentCount.calendar)
        assertTrue(conversation.attachments.any { it.mimeType.category == MimeTypeCategory.Calendar })
        assertTrue(conversation.attachments.any { it.mimeType.category == MimeTypeCategory.Image })
        assertTrue(conversation.attachments.any { it.mimeType.category == MimeTypeCategory.Pdf })
    }

    @Test
    fun `Local conversation with inline attachments to conversation should convert correctly`() {
        // Given
        val attachments = listOf(
            LocalAttachmentMetadataSample.Pdf,
            LocalAttachmentMetadataSample.Compressed,
            LocalAttachmentMetadataSample.InlineImage
        )
        val localConversation = createLocalConversation(
            attachmentsMetadata = attachments
        )

        // When
        val conversation = localConversation.toConversation()

        // Then
        assertEquals(2, conversation.attachments.size)
        assertEquals(0, conversation.attachmentCount.calendar)
    }


    @Test
    fun `InlineCustomLabel to ConversationLabel should convert correctly`() {
        // Given
        val name = "Test Label"
        val color = LabelColor("0xFF0000")
        val customLabel = InlineCustomLabel(
            id = LocalLabelId(1uL),
            name = name,
            color = color
        )

        // When
        val label = customLabel.toLabel()

        // Then
        assertEquals(LabelId(customLabel.id.value.toString()), label.labelId)
        assertEquals(color.value, label.color)
        assertEquals(name, label.name)
    }

    @Test
    fun `conversation with displaySnoozeReminder  should convert correctly`() {
        // Given
        val expected = SnoozeReminder

        val conversation = createLocalConversation(displaySnoozeReminder = true).toConversation()

        // Then
        assertEquals(expected, conversation.snoozeInformation)
    }

    @Test
    fun `conversation with hiddenMessagesBanner should convert correctly`() {
        // Given
        val expected = ch.protonmail.android.mailconversation.domain.entity.HiddenMessagesBanner.ContainsTrashedMessages

        val conversation = createLocalConversation(
            hiddenMessagesBanner = HiddenMessagesBanner.CONTAINS_TRASHED_MESSAGES
        ).toConversation()

        // Then
        assertEquals(expected, conversation.hiddenMessagesBanner)
    }

    private fun createLocalConversation(
        id: LocalConversationId = LocalConversationId(123uL),
        displayOrder: ULong = 1uL,
        subject: String = "Test Subject",
        senders: List<MessageSender> = listOf(
            MessageSender("sender1@test.com", "Sender1", true, false, false, ""),
            MessageSender("sender2@test.com", "Sender2", false, false, false, "")
        ),
        recipients: List<MessageRecipient> = listOf(
            MessageRecipient("recipient1@test.com", true, "Recipient1", null),
            MessageRecipient("recipient2@test.com", false, "Recipient2", null)
        ),
        numMessages: ULong = 5uL,
        numUnread: ULong = 2uL,
        attachmentsMetadata: List<LocalAttachmentMetadata> = emptyList(),
        expirationTime: ULong = 1625235000000uL,
        size: ULong = 1024uL,
        time: ULong = 1625250000000uL,
        customLabels: List<InlineCustomLabel> = listOf(
            InlineCustomLabel(LocalLabelId(1uL), "Test Label", LabelColor("0xFF0000"))
        ),
        isStarred: Boolean = false,
        displaySnoozeReminder: Boolean = false,
        exclusiveLocation: LocalExclusiveLocationSystem = LocalExclusiveLocationSystem(
            name = SystemLabel.TRASH,
            id = LocalLabelId(1uL)
        ),
        avatar: AvatarInformation = AvatarInformation("A", "blue"),
        totalMessages: ULong = 8uL,
        totalUnread: ULong = 3uL,
        hiddenMessagesBanner: LocalHiddenMessagesBanner? = null
    ): LocalConversation {
        return LocalConversation(
            id = id,
            displayOrder = displayOrder,
            subject = subject,
            senders = senders,
            recipients = recipients,
            numMessages = numMessages,
            numUnread = numUnread,
            numAttachments = attachmentsMetadata.size.toULong(),
            expirationTime = expirationTime,
            size = size,
            time = time,
            customLabels = customLabels,
            isStarred = isStarred,
            attachmentsMetadata = attachmentsMetadata,
            displaySnoozeReminder = displaySnoozeReminder,
            locations = listOf(exclusiveLocation),
            avatar = avatar,
            totalMessages = totalMessages,
            totalUnread = totalUnread,
            snoozedUntil = null,
            hiddenMessagesBanner = hiddenMessagesBanner
        )
    }

}
