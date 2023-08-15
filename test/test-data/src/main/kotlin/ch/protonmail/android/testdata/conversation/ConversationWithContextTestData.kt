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
import ch.protonmail.android.mailmessage.domain.model.AttachmentCount
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import ch.protonmail.android.testdata.user.UserIdTestData.userId1
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

object ConversationWithContextTestData {

    val conversation1 = buildConversation(userId = userId, id = "1", time = 1000).withContext()
    val conversation2 = buildConversation(userId = userId, id = "2", time = 2000).withContext()
    val conversation3 = buildConversation(userId = userId, id = "3", time = 3000).withContext()
    val conversation4 = buildConversation(userId = userId, id = "4", time = 4000).withContext()

    val conversation1NoLabels = buildConversation(
        userId = userId, id = "1", time = 1000, labelIds = emptyList()
    ).withContext()
    val conversation1Labeled = buildConversation(
        userId = userId, id = "1", time = 1000, labelIds = listOf("0")
    ).withContext()
    val conversation2Labeled = buildConversation(
        userId = userId, id = "2", time = 2000, labelIds = listOf("4")
    ).withContext()
    val conversation3Labeled = buildConversation(
        userId = userId, id = "3", time = 3000, labelIds = listOf("0", "1")
    ).withContext()

    val conversation1Ordered = buildConversation(userId = userId, id = "1", order = 1000).withContext()
    val conversation2Ordered = buildConversation(userId = userId, id = "2", order = 2000).withContext()

    object User2 {

        val conversation1Labeled = buildConversation(
            userId = userId1, id = "1", time = 1000, labelIds = listOf("0")
        ).withContext()
        val conversation2Labeled = buildConversation(
            userId = userId1, id = "2", time = 2000, labelIds = listOf("4")
        ).withContext()
        val conversation3Labeled = buildConversation(
            userId = userId1, id = "3", time = 3000, labelIds = listOf("0", "1")
        ).withContext()
    }

    fun getConversationWithContext(
        userId: UserId,
        id: String,
        order: Long = 0,
        time: Long = 0,
        labelIds: List<String> = listOf("0"),
        contextLabelId: LabelId = LabelId("0"),
        numAttachments: Int = 0,
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

    private fun buildConversation(
        userId: UserId,
        id: String,
        order: Long = 1000,
        time: Long = 1000,
        labelIds: List<String> = listOf(id),
        numAttachments: Int = 2,
        expirationTime: Long = 0,
        attachmentCount: AttachmentCount = AttachmentCount(0)
    ) = Conversation(
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
    )

    private fun Conversation.withContext(contextLabelId: LabelId = LabelId("0")) = ConversationWithContext(
        this,
        contextLabelId
    )

}
