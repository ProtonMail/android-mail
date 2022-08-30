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
import ch.protonmail.android.mailmessage.domain.entity.AttachmentCount
import ch.protonmail.android.mailmessage.domain.entity.Recipient
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

object ConversationTestData {

    fun buildConversation(
        userId: UserId,
        id: String,
        order: Long = 1000,
        time: Long = 1000,
        labelIds: List<String> = listOf("0"),
        numAttachments: Int = 0,
        expirationTime: Long = 0
    ) = Conversation(
        userId = userId,
        conversationId = ConversationId(id),
        contextLabelId = labelIds.firstOrNull()?.let { LabelId(it) } ?: LabelId("0"),
        order = order,
        labels = labelIds.map { buildConversationLabel(id, it, time) },
        subject = "subject",
        senders = listOf(Recipient("address", "name")),
        recipients = emptyList(),
        expirationTime = expirationTime,
        numMessages = 1,
        numUnread = 0,
        numAttachments = numAttachments,
        attachmentCount = AttachmentCount(0)
    )

    private fun buildConversationLabel(
        conversationId: String,
        labelId: String,
        time: Long = 1000,
        size: Long = 1000,
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
