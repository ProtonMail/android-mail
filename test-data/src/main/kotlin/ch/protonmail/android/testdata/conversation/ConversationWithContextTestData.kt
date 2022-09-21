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
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithContext
import ch.protonmail.android.mailmessage.domain.entity.AttachmentCount
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

object ConversationWithContextTestData {

    fun getConversationWithContext(
        userId: UserId = UserId("1"),
        id: String = "1",
        order: Long = 1000,
        time: Long = 1000,
        labelIds: List<String> = listOf(id),
        contextLabelId: LabelId = LabelId("0"),
        numAttachments: Int = 2,
        expirationTime: Long = 0,
        attachmentCount: AttachmentCount = AttachmentCount(0)
    ) = ConversationWithContext(
        conversation = Conversation(
            userId = userId,
            conversationId = ConversationId(id),
            order = order,
            labels = labelIds.map {
                ConversationLabel(ConversationId(id), LabelId(it), time, 0, 0, 0, 0)
            },
            subject = "subject",
            senders = emptyList(),
            recipients = emptyList(),
            expirationTime = expirationTime,
            numMessages = 1,
            numUnread = 0,
            numAttachments = numAttachments,
            attachmentCount = attachmentCount
        ),
        contextLabelId = contextLabelId
    )

}
