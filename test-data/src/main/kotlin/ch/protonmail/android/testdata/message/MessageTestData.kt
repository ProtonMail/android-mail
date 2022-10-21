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

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.entity.AttachmentCount
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.Recipient
import ch.protonmail.android.testdata.user.UserIdTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.user.domain.entity.AddressId

object MessageTestData {

    const val RAW_MESSAGE_ID = "rawMessageId"
    const val RAW_SUBJECT = "Here's a new message"

    val message = buildMessage(
        userId = userId,
        id = RAW_MESSAGE_ID,
        subject = RAW_SUBJECT,
        labelIds = listOf(SystemLabelId.Inbox.labelId.id)
    )

    val multipleRecipientsMessage = buildMessage(
        userId = userId,
        id = RAW_MESSAGE_ID,
        subject = RAW_SUBJECT,
        labelIds = listOf(SystemLabelId.Inbox.labelId.id),
        toList = listOf(
            Recipient("recipient1@pm.me", "recipient1"),
            Recipient("recipient2@pm.me", "recipient2")
        )
    )

    val trashedMessage = buildMessage(
        userId = userId,
        id = RAW_MESSAGE_ID,
        subject = RAW_SUBJECT,
        labelIds = listOf(SystemLabelId.Trash.labelId.id)
    )

    val trashedMessageWithCustomLabels = buildMessage(
        userId = userId,
        id = RAW_MESSAGE_ID,
        subject = RAW_SUBJECT,
        labelIds = listOf(
            SystemLabelId.Trash.labelId.id,
            MailLabelId.Custom.Label(LabelId("Travel")).labelId.id
        )
    )

    val spamMessage = buildMessage(
        userId = userId,
        id = RAW_MESSAGE_ID,
        subject = RAW_SUBJECT,
        labelIds = listOf(SystemLabelId.Spam.labelId.id)
    )

    val spamMessageWithMultipleRecipients = buildMessage(
        userId = userId,
        id = RAW_MESSAGE_ID,
        subject = RAW_SUBJECT,
        labelIds = listOf(SystemLabelId.Spam.labelId.id),
        toList = listOf(Recipient("recipient1@pm.me", "recipient1")),
        ccList = listOf(Recipient("recipient2@pm.me", "recipient2"))
    )

    fun buildMessage(
        userId: UserId = UserIdTestData.userId,
        id: String,
        order: Long = 1000,
        time: Long = 1000,
        labelIds: List<String> = listOf("0"),
        subject: String = "subject",
        numAttachments: Int = 0,
        expirationTime: Long = 0,
        attachmentCount: AttachmentCount = AttachmentCount(0),
        toList: List<Recipient> = emptyList(),
        ccList: List<Recipient> = emptyList()
    ) = Message(
        userId = userId,
        messageId = MessageId(id),
        conversationId = ConversationId(id),
        time = time,
        size = 0,
        order = order,
        labelIds = labelIds.map { LabelId(it) },
        subject = subject,
        unread = false,
        sender = Recipient("address", "name"),
        toList = toList,
        ccList = ccList,
        bccList = emptyList(),
        expirationTime = expirationTime,
        isReplied = false,
        isRepliedAll = false,
        isForwarded = false,
        addressId = AddressId("1"),
        externalId = null,
        numAttachments = numAttachments,
        flags = 0,
        attachmentCount = attachmentCount
    )
}
