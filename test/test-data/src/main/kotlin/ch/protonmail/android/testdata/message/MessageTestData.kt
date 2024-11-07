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

package ch.protonmail.android.testdata.message

import java.time.Instant
import java.time.temporal.ChronoUnit
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId.AllDrafts
import ch.protonmail.android.maillabel.domain.model.SystemLabelId.AllMail
import ch.protonmail.android.maillabel.domain.model.SystemLabelId.AllScheduled
import ch.protonmail.android.maillabel.domain.model.SystemLabelId.AllSent
import ch.protonmail.android.maillabel.domain.model.SystemLabelId.AlmostAllMail
import ch.protonmail.android.maillabel.domain.model.SystemLabelId.Outbox
import ch.protonmail.android.maillabel.domain.model.SystemLabelId.Snoozed
import ch.protonmail.android.mailmessage.domain.model.AttachmentCount
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.model.Sender
import ch.protonmail.android.testdata.user.UserIdTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.user.domain.entity.AddressId

object MessageTestData {

    const val RAW_MESSAGE_ID = "rawMessageId"
    const val RAW_SUBJECT = "Here's a new message"

    val sender = Sender("sender@pm.me", "Sender")
    val recipient1 = Recipient("recipient1@pm.me", "Recipient1")
    val recipient2 = Recipient("recipient2@pm.me", "Recipient2")
    val recipient3 = Recipient("recipient3@pm.me", "Recipient3")

    // Do not reference production code directly for this, otherwise tests will never fail in case of changes.
    val unmodifiableLabels = listOf(AllMail, AlmostAllMail, AllDrafts, AllSent, AllScheduled, Outbox, Snoozed)

    val message = buildMessage(
        userId = userId, id = RAW_MESSAGE_ID, subject = RAW_SUBJECT, labelIds = listOf(SystemLabelId.Inbox.labelId.id)
    )

    val multipleRecipientsMessage = buildMessage(
        userId = userId,
        id = RAW_MESSAGE_ID,
        subject = RAW_SUBJECT,
        labelIds = listOf(SystemLabelId.Inbox.labelId.id),
        toList = listOf(recipient1, recipient2)
    )

    val trashedMessage = buildMessage(
        userId = userId, id = RAW_MESSAGE_ID, subject = RAW_SUBJECT, labelIds = listOf(SystemLabelId.Trash.labelId.id)
    )

    val trashedMessageWithCustomLabels = buildMessage(
        userId = userId, id = RAW_MESSAGE_ID, subject = RAW_SUBJECT,
        labelIds = listOf(
            SystemLabelId.Trash.labelId.id, MailLabelId.Custom.Label(LabelId("Travel")).labelId.id
        )
    )

    val spamMessage = buildMessage(
        userId = userId, id = RAW_MESSAGE_ID, subject = RAW_SUBJECT, labelIds = listOf(SystemLabelId.Spam.labelId.id)
    )

    val spamMessageWithMultipleRecipients = buildMessage(
        userId = userId,
        id = RAW_MESSAGE_ID,
        subject = RAW_SUBJECT,
        labelIds = listOf(SystemLabelId.Spam.labelId.id),
        toList = listOf(recipient1),
        ccList = listOf(recipient2)
    )

    val starredMessageInArchiveWithAttachments = buildMessage(
        userId = userId,
        id = RAW_MESSAGE_ID,
        sender = sender,
        subject = RAW_SUBJECT,
        labelIds = listOf(
            SystemLabelId.Archive.labelId.id, SystemLabelId.AllMail.labelId.id, SystemLabelId.Starred.labelId.id
        ),
        numAttachments = 2,
        attachmentCount = AttachmentCount(1),
        time = 1_667_924_198L,
        toList = listOf(recipient1, recipient2),
        ccList = listOf(recipient3)
    )

    val starredMessage = buildMessage(
        userId = userId, id = RAW_MESSAGE_ID, subject = RAW_SUBJECT,
        labelIds = listOf(
            SystemLabelId.Inbox.labelId.id, SystemLabelId.Starred.labelId.id
        )
    )

    val unStarredMessagesByConversation = listOf(
        buildMessage(id = "123", conversationId = ConversationId("conversation")),
        buildMessage(id = "124", conversationId = ConversationId("conversation")),
        buildMessage(id = "125", conversationId = ConversationId("conversation"))
    )
    val unStarredMsgByConversationWithStarredMsg = listOf(
        buildMessage(id = "123", conversationId = ConversationId("conversation")),
        buildMessage(id = "124", conversationId = ConversationId("conversation")),
        buildMessage(
            id = "125", conversationId = ConversationId("conversation"),
            labelIds = listOf(
                SystemLabelId.Inbox.labelId.id, SystemLabelId.Starred.labelId.id
            )
        )
    )

    val starredMessagesWithPartiallySetLabels = listOf(
        buildMessage(
            id = "123",
            labelIds = listOf(SystemLabelId.Inbox.labelId.id, SystemLabelId.Starred.labelId.id, LabelId("11").id)
        ),
        buildMessage(
            id = "124",
            labelIds = listOf(SystemLabelId.Inbox.labelId.id, SystemLabelId.Starred.labelId.id, LabelId("11").id)
        ),
        buildMessage(
            id = "125",
            labelIds = listOf(SystemLabelId.Inbox.labelId.id, SystemLabelId.Starred.labelId.id)
        )
    )

    val starredMessagesWithCustomLabel = listOf(
        buildMessage(
            id = "123",
            labelIds = listOf(SystemLabelId.Inbox.labelId.id, SystemLabelId.Starred.labelId.id, LabelId("11").id)
        ),
        buildMessage(
            id = "124",
            labelIds = listOf(SystemLabelId.Inbox.labelId.id, SystemLabelId.Starred.labelId.id, LabelId("11").id)
        ),
        buildMessage(
            id = "125",
            labelIds = listOf(SystemLabelId.Inbox.labelId.id, SystemLabelId.Starred.labelId.id, LabelId("11").id)
        )
    )

    val starredMsgByConversationWithCustomLabel = starredMessagesWithCustomLabel.map {
        it.copy(conversationId = ConversationId("conversation"))
    }

    val starredMessagesByConversation = listOf(
        buildMessage(
            id = "123", conversationId = ConversationId("conversation"),
            labelIds = listOf(
                SystemLabelId.Inbox.labelId.id, SystemLabelId.Starred.labelId.id
            )
        ),
        buildMessage(
            id = "124", conversationId = ConversationId("conversation"),
            labelIds = listOf(
                SystemLabelId.Inbox.labelId.id, SystemLabelId.Starred.labelId.id
            )
        ),
        buildMessage(
            id = "125", conversationId = ConversationId("conversation"),
            labelIds = listOf(
                SystemLabelId.Inbox.labelId.id, SystemLabelId.Starred.labelId.id
            )
        )
    )
    val starredMsgByConversationWithUnStarredMsg = listOf(
        buildMessage(
            id = "123", conversationId = ConversationId("conversation"),
            labelIds = listOf(
                SystemLabelId.Inbox.labelId.id, SystemLabelId.Starred.labelId.id
            )
        ),
        buildMessage(
            id = "124", conversationId = ConversationId("conversation"),
            labelIds = listOf(
                SystemLabelId.Inbox.labelId.id, SystemLabelId.Starred.labelId.id
            )
        ),
        buildMessage(
            id = "124", conversationId = ConversationId("conversation"),
            labelIds = listOf(
                SystemLabelId.Inbox.labelId.id
            )
        )
    )

    val autoPhishingMessage = buildMessage(id = "message", flags = Message.FLAG_PHISHING_AUTO)

    val expiringMessage = buildMessage(
        id = "message",
        expirationTime = Instant.now().plus(1, ChronoUnit.HOURS).epochSecond,
        flags = 0L or Message.FLAG_EXPIRATION_FROZEN
    )

    val autoDeleteMessage = buildMessage(
        id = "message",
        expirationTime = Instant.now().plus(1, ChronoUnit.HOURS).epochSecond
    )

    fun buildMessage(
        userId: UserId = UserIdTestData.userId,
        id: String,
        order: Long = 1000,
        time: Long = 1000,
        size: Long = 0,
        labelIds: List<String> = listOf(SystemLabelId.Inbox.labelId.id),
        subject: String = "subject",
        sender: Sender = Sender("address", "name"),
        numAttachments: Int = 0,
        expirationTime: Long = 0,
        attachmentCount: AttachmentCount = AttachmentCount(0),
        toList: List<Recipient> = emptyList(),
        ccList: List<Recipient> = emptyList(),
        bccList: List<Recipient> = emptyList(),
        conversationId: ConversationId = ConversationId(id),
        flags: Long = 0
    ) = Message(
        userId = userId,
        messageId = MessageId(id),
        conversationId = conversationId,
        time = time,
        size = size,
        order = order,
        labelIds = labelIds.map { LabelId(it) },
        subject = subject,
        unread = false,
        sender = sender,
        toList = toList,
        ccList = ccList,
        bccList = bccList,
        expirationTime = expirationTime,
        isReplied = false,
        isRepliedAll = false,
        isForwarded = false,
        addressId = AddressId("1"),
        externalId = null,
        numAttachments = numAttachments,
        flags = flags,
        attachmentCount = attachmentCount
    )
}
