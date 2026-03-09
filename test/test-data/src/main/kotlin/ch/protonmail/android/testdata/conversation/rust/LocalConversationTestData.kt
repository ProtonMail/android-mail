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

package ch.protonmail.android.testdata.conversation.rust

import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentMetadata
import ch.protonmail.android.mailcommon.data.mapper.LocalConversation
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import uniffi.mail_uniffi.AvatarInformation
import uniffi.mail_uniffi.InlineCustomLabel
import uniffi.mail_uniffi.LabelColor
import uniffi.mail_uniffi.MessageRecipient
import uniffi.mail_uniffi.MessageSender

object LocalConversationTestData {

    const val RAW_SUBJECT = "Conversation Subject"
    const val RAW_CONVERSATION_ID = 1000uL

    val sender = MessageSender(
        address = "sender@pm.me",
        name = "Sender",
        isProton = true,
        displaySenderImage = false,
        isSimpleLogin = false,
        bimiSelector = null
    )

    val recipient1 = MessageRecipient(
        address = "recipient1@pm.me", name = "Recipient1",
        isProton = true,
        group = null
    )
    val recipient2 = MessageRecipient(
        address = "recipient2@pm.me", name = "Recipient2",
        isProton = true,
        group = null
    )
    val recipient3 = MessageRecipient(
        address = "recipient3@pm.me", name = "Recipient3",
        isProton = true,
        group = null
    )

    val AugConversation = buildConversation(
        id = LocalConversationIdSample.AugConversation,
        subject = "August conversation",
        senders = listOf(sender),
        recipients = listOf(recipient1),
        time = 1667924198uL
    )

    val SepConversation = buildConversation(
        id = LocalConversationIdSample.SepConversation,
        subject = "September conversation",
        senders = listOf(sender),
        recipients = listOf(recipient1),
        time = 1667924198uL
    )

    val OctConversation = buildConversation(
        id = LocalConversationIdSample.OctConversation,
        subject = "October conversation",
        senders = listOf(sender),
        recipients = listOf(recipient1),
        time = 1667924198uL
    )

    val multipleRecipientsConversation = buildConversation(
        id = LocalConversationId(RAW_CONVERSATION_ID),
        subject = "Multiple recipients conversation",
        senders = listOf(sender),
        recipients = listOf(recipient1, recipient2),
        time = 1667924198uL
    )

    val trashedConversation = buildConversation(
        id = LocalConversationId(RAW_CONVERSATION_ID),
        subject = "Trashed conversation",
        senders = listOf(sender),
        recipients = listOf(recipient1),
        labels = listOf(InlineCustomLabel(LocalLabelId(2uL), "Trash", LabelColor("red"))),
        time = 1667924198uL
    )

    val spamConversation = buildConversation(
        id = LocalConversationId(RAW_CONVERSATION_ID),
        subject = RAW_SUBJECT,
        senders = listOf(sender),
        recipients = listOf(recipient1),
        labels = listOf(InlineCustomLabel(LocalLabelId(4uL), "Spam", LabelColor("yellow"))),
        time = 1667924198uL
    )

    val spamConversationWithMultipleRecipients = buildConversation(
        id = LocalConversationId(RAW_CONVERSATION_ID),
        subject = RAW_SUBJECT,
        senders = listOf(sender),
        recipients = listOf(recipient1),
        labels = listOf(InlineCustomLabel(LocalLabelId(4uL), "Spam", LabelColor("yellow"))),
        time = 1667924198uL
    )

    val starredConversation = buildConversation(
        id = LocalConversationId(RAW_CONVERSATION_ID),
        subject = RAW_SUBJECT,
        senders = listOf(sender),
        recipients = listOf(recipient1),
        labels = listOf(
            InlineCustomLabel(LocalLabelId(1uL), "Inbox", LabelColor("green")),
            InlineCustomLabel(LocalLabelId(5uL), "Starred", LabelColor("yellow"))
        ),
        time = 1667924198uL
    )

    val starredConversationsWithCustomLabel = listOf(
        buildConversation(
            id = LocalLabelId(123uL),
            subject = "Conversation 123",
            senders = listOf(sender),
            recipients = listOf(recipient1),
            labels = listOf(
                InlineCustomLabel(LocalLabelId(1uL), "Inbox", LabelColor("green")),
                InlineCustomLabel(LocalLabelId(5uL), "Starred", LabelColor("yellow")),
                InlineCustomLabel(LocalLabelId(11uL), "Custom", LabelColor("blue"))
            ),
            time = 1667924198uL
        ),
        buildConversation(
            id = LocalConversationId(124uL),
            subject = "Conversation 124",
            senders = listOf(sender),
            recipients = listOf(recipient2),
            labels = listOf(
                InlineCustomLabel(LocalLabelId(1uL), "Inbox", LabelColor("green")),
                InlineCustomLabel(LocalLabelId(5uL), "Starred", LabelColor("yellow")),
                InlineCustomLabel(LocalLabelId(11uL), "Custom", LabelColor("blue"))
            ),
            time = 1667924198uL
        ),
        buildConversation(
            id = LocalConversationId(125uL),
            subject = "Conversation 125",
            senders = listOf(sender),
            recipients = listOf(recipient3),
            labels = listOf(
                InlineCustomLabel(LocalLabelId(1uL), "Inbox", LabelColor("green")),
                InlineCustomLabel(LocalLabelId(5uL), "Starred", LabelColor("yellow"))
            ),
            time = 1667924198uL
        )
    )

    fun buildConversation(
        id: LocalConversationId,
        subject: String,
        senders: List<MessageSender>,
        recipients: List<MessageRecipient>,
        labels: List<InlineCustomLabel> = listOf(InlineCustomLabel(LocalLabelId(1uL), "Inbox", LabelColor("green"))),
        time: ULong,
        size: ULong = 0uL,
        expirationTime: ULong = 0uL,
        snoozeTime: ULong = 0uL,
        numMessages: ULong = 1uL,
        numMessagesCtx: ULong = 1uL,
        numUnread: ULong = 0uL,
        numAttachments: ULong = 0uL,
        starred: Boolean = false,
        attachments: List<LocalAttachmentMetadata> = emptyList(),
        avatarInformation: AvatarInformation = AvatarInformation("A", "blue")
    ) = LocalConversation(
        id = id,
        displayOrder = 1uL,
        subject = subject,
        senders = senders,
        recipients = recipients,
        numMessages = numMessagesCtx,
        numUnread = numUnread,
        numAttachments = numAttachments,
        expirationTime = expirationTime,
        size = size,
        time = time,
        customLabels = labels,
        isStarred = starred,
        displaySnoozeReminder = false,
        locations = emptyList(),
        avatar = avatarInformation,
        attachmentsMetadata = attachments,
        totalMessages = numMessages,
        totalUnread = numUnread,
        snoozedUntil = 0uL,
        hiddenMessagesBanner = null
    )
}
