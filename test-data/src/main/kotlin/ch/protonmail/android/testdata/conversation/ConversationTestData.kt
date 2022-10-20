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

package ch.protonmail.android.testdata.conversation

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.entity.AttachmentCount
import ch.protonmail.android.mailmessage.domain.entity.Recipient
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

object ConversationTestData {

    const val RAW_CONVERSATION_ID = "rawConversationId"
    const val RAW_SUBJECT = "Here's a new email"

    val conversation = buildConversation(
        userId = userId,
        id = RAW_CONVERSATION_ID,
        subject = RAW_SUBJECT,
        labelIds = listOf(SystemLabelId.Inbox.labelId.id),
        numMessages = 1
    )

    val conversationWith3Messages = buildConversation(
        userId = userId,
        id = RAW_CONVERSATION_ID,
        subject = RAW_SUBJECT,
        numMessages = 3
    )

    val starredConversation = buildConversation(
        userId = userId,
        id = RAW_CONVERSATION_ID,
        subject = RAW_SUBJECT,
        labelIds = listOf(SystemLabelId.Starred.labelId.id)
    )

    val trashAndSpamConversation = buildConversation(
        userId = userId,
        id = RAW_CONVERSATION_ID,
        subject = RAW_SUBJECT,
        labelIds = listOf(
            SystemLabelId.Trash.labelId.id,
            SystemLabelId.Spam.labelId.id
        )
    )

    val trashConversationWithCustomLabels = buildConversation(
        userId = userId,
        id = RAW_CONVERSATION_ID,
        subject = RAW_SUBJECT,
        labelIds = listOf(
            SystemLabelId.Trash.labelId.id,
            MailLabelId.Custom.Label(LabelId("Travel")).labelId.id
        )
    )

    private fun buildConversation(
        userId: UserId,
        id: String,
        subject: String,
        numMessages: Int = 1,
        labelIds: List<String> = listOf("0"),
        numAttachments: Int = 0,
        expirationTime: Long = 0,
        attachmentCount: AttachmentCount = AttachmentCount(0)
    ) = Conversation(
        userId = userId,
        conversationId = ConversationId(id),
        order = 0,
        labels = labelIds.map { buildConversationLabel(id, it) },
        subject = subject,
        senders = listOf(Recipient("address", "name")),
        recipients = emptyList(),
        expirationTime = expirationTime,
        numMessages = numMessages,
        numUnread = 0,
        numAttachments = numAttachments,
        attachmentCount = attachmentCount
    )

    private fun buildConversationLabel(
        conversationId: String,
        labelId: String,
        time: Long = 0,
        size: Long = 0
    ) = ConversationLabel(
        conversationId = ConversationId(conversationId),
        labelId = LabelId(labelId),
        contextTime = time,
        contextSize = size,
        contextNumMessages = 0,
        contextNumUnread = 0,
        contextNumAttachments = 0
    )
}
