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

package ch.protonmail.android.testdata.message.rust

import ch.protonmail.android.mailcommon.data.mapper.LocalAddressId
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentMetadata
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageId
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageMetadata
import uniffi.mail_uniffi.AvatarInformation
import uniffi.mail_uniffi.InlineCustomLabel
import uniffi.mail_uniffi.LabelColor
import uniffi.mail_uniffi.MessageFlags
import uniffi.mail_uniffi.MessageRecipient
import uniffi.mail_uniffi.MessageSender

object LocalMessageTestData {
    const val RAW_SUBJECT = "Subject"
    const val RAW_MESSAGE_ID = 1000uL

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

    val AugWeatherForecast = buildMessage(
        id = LocalMessageIdSample.AugWeatherForecast,
        subject = "August weather forecast",
        sender = sender,
        to = listOf(recipient1),
        cc = emptyList(),
        bcc = emptyList(),
        time = 1667924198uL
    )

    val SepWeatherForecast = buildMessage(
        id = LocalMessageIdSample.SepWeatherForecast,
        subject = "September weather forecast",
        sender = sender,
        to = listOf(recipient1),
        cc = emptyList(),
        bcc = emptyList(),
        time = 1667924198uL
    )

    val OctWeatherForecast = buildMessage(
        id = LocalMessageIdSample.OctWeatherForecast,
        subject = "October weather forecast",
        sender = sender,
        to = listOf(recipient1),
        cc = emptyList(),
        bcc = emptyList(),
        time = 1667924198uL
    )


    val multipleRecipientsMessage = buildMessage(
        id = LocalMessageId(RAW_MESSAGE_ID),
        subject = "Multiple recipients message",
        sender = sender,
        to = listOf(recipient1, recipient2),
        cc = emptyList(),
        bcc = emptyList(),
        time = 1667924198uL
    )

    val trashedMessage = buildMessage(
        id = LocalMessageId(RAW_MESSAGE_ID),
        subject = "Trashed message",
        sender = sender,
        to = listOf(recipient1),
        cc = emptyList(),
        bcc = emptyList(),
        labels = listOf(InlineCustomLabel(LocalLabelId(2uL), "Trash", LabelColor("red"))),
        time = 1667924198uL
    )

    val trashedMessageWithInlineCustomLabels = buildMessage(
        id = LocalMessageId(RAW_MESSAGE_ID),
        subject = "Trashed message with custom labels",
        sender = sender,
        to = listOf(recipient1),
        cc = emptyList(),
        bcc = emptyList(),
        labels = listOf(
            InlineCustomLabel(LocalLabelId(2uL), "Trash", LabelColor("red")),
            InlineCustomLabel(LocalLabelId(3uL), "Travel", LabelColor("blue"))
        ),
        time = 1667924198uL
    )

    val spamMessage = buildMessage(
        id = LocalMessageId(RAW_MESSAGE_ID),
        subject = RAW_SUBJECT,
        sender = sender,
        to = listOf(recipient1),
        cc = emptyList(),
        bcc = emptyList(),
        labels = listOf(InlineCustomLabel(LocalLabelId(4uL), "Spam", LabelColor("yellow"))),
        time = 1667924198uL
    )

    val spamMessageWithMultipleRecipients = buildMessage(
        id = LocalMessageId(RAW_MESSAGE_ID),
        subject = RAW_SUBJECT,
        sender = sender,
        to = listOf(recipient1),
        cc = listOf(recipient2),
        bcc = emptyList(),
        labels = listOf(InlineCustomLabel(LocalLabelId(4uL), "Spam", LabelColor("yellow"))),
        time = 1667924198uL
    )

    val starredMessage = buildMessage(
        id = LocalMessageId(RAW_MESSAGE_ID),
        subject = RAW_SUBJECT,
        sender = sender,
        to = listOf(recipient1),
        cc = emptyList(),
        bcc = emptyList(),
        labels = listOf(
            InlineCustomLabel(LocalLabelId(1uL), "Inbox", LabelColor("green")),
            InlineCustomLabel(LocalLabelId(5uL), "Starred", LabelColor("yellow"))
        ),
        time = 1667924198uL
    )

    val starredMessagesWithInlineCustomLabel = listOf(
        buildMessage(
            id = LocalMessageId(123uL),
            subject = "Message 123",
            sender = sender,
            to = listOf(recipient1),
            labels = listOf(
                InlineCustomLabel(LocalLabelId(1uL), "Inbox", LabelColor("green")),
                InlineCustomLabel(LocalLabelId(5uL), "Starred", LabelColor("yellow")),
                InlineCustomLabel(LocalLabelId(11uL), "Custom", LabelColor("blue"))
            ),
            time = 1667924198uL
        ),
        buildMessage(
            id = LocalMessageId(124uL),
            subject = "Message 124",
            sender = sender,
            to = listOf(recipient2),
            labels = listOf(
                InlineCustomLabel(LocalLabelId(1uL), "Inbox", LabelColor("green")),
                InlineCustomLabel(LocalLabelId(5uL), "Starred", LabelColor("yellow")),
                InlineCustomLabel(LocalLabelId(11uL), "Custom", LabelColor("blue"))
            ),
            time = 1667924198uL
        ),
        buildMessage(
            id = LocalMessageId(125uL),
            subject = "Message 125",
            sender = sender,
            to = listOf(recipient3),
            labels = listOf(
                InlineCustomLabel(LocalLabelId(1uL), "Inbox", LabelColor("green")),
                InlineCustomLabel(LocalLabelId(5uL), "Starred", LabelColor("yellow"))
            ),
            time = 1667924198uL
        )
    )

    fun buildMessage(
        id: LocalMessageId,
        subject: String,
        sender: MessageSender,
        to: List<MessageRecipient>,
        cc: List<MessageRecipient> = emptyList(),
        bcc: List<MessageRecipient> = emptyList(),
        labels: List<InlineCustomLabel> = listOf(InlineCustomLabel(LocalLabelId(1uL), "Inbox", LabelColor("green"))),
        time: ULong,
        size: ULong = 0uL,
        expirationTime: ULong = 0uL,
        snoozeTime: ULong = 0uL,
        isReplied: Boolean = false,
        isRepliedAll: Boolean = false,
        isForwarded: Boolean = false,
        numAttachments: UInt = 0u,
        flags: MessageFlags = MessageFlags(0uL),
        starred: Boolean = false,
        attachments: List<LocalAttachmentMetadata> = emptyList(),
        avatarInformation: AvatarInformation = AvatarInformation("A", "blue")
    ) = LocalMessageMetadata(
        id = id,
        conversationId = LocalConversationId(50.toULong()),
        addressId = LocalAddressId(1.toULong()),
        displayOrder = 1uL,
        subject = subject,
        unread = false,
        sender = sender,
        time = time,
        size = size,
        expirationTime = expirationTime,
        snoozedUntil = snoozeTime,
        isReplied = isReplied,
        isRepliedAll = isRepliedAll,
        isForwarded = isForwarded,
        numAttachments = numAttachments,
        flags = flags,
        starred = starred,
        toList = to,
        ccList = cc,
        bccList = bcc,
        attachmentsMetadata = attachments,
        customLabels = labels,
        location = null,
        avatar = avatarInformation,
        isDraft = false,
        isScheduled = false,
        canReply = false,
        displaySnoozeReminder = false
    )
}
