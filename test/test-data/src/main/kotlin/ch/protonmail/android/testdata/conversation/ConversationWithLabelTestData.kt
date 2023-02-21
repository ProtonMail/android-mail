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
import ch.protonmail.android.mailconversation.data.local.entity.ConversationEntity
import ch.protonmail.android.mailconversation.data.local.relation.ConversationWithLabels
import ch.protonmail.android.mailconversation.domain.entity.ConversationLabel
import ch.protonmail.android.mailmessage.data.local.entity.AttachmentCountEntity
import ch.protonmail.android.testdata.user.UserIdTestData
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

object ConversationWithLabelTestData {

    fun conversationWithLabel(
        userId: UserId = UserIdTestData.userId,
        conversationId: ConversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
    ) = ConversationWithLabels(
        conversation = ConversationEntity(
            userId = userId,
            conversationId = conversationId,
            order = 1000,
            subject = "subject",
            senders = emptyList(),
            recipients = emptyList(),
            expirationTime = 0,
            numMessages = 1,
            numUnread = 0,
            numAttachments = 2,
            attachmentCount = AttachmentCountEntity(
                calendar = 0
            )
        ),
        labels = listOf(
            ConversationLabel(
                conversationId = conversationId,
                labelId = LabelId("0"),
                contextTime = 1000,
                contextSize = 0,
                contextNumMessages = 0,
                contextNumUnread = 0,
                contextNumAttachments = 0
            )
        )
    )

    fun conversationWithMultipleLabels(
        userId: UserId = UserIdTestData.userId,
        conversationId: ConversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
    ) = ConversationWithLabels(
        conversation = ConversationEntity(
            userId = userId,
            conversationId = conversationId,
            order = 1000,
            subject = "subject",
            senders = emptyList(),
            recipients = emptyList(),
            expirationTime = 0,
            numMessages = 1,
            numUnread = 0,
            numAttachments = 2,
            attachmentCount = AttachmentCountEntity(
                calendar = 0
            )
        ),
        labels = listOf(
            ConversationLabel(
                conversationId = conversationId,
                labelId = LabelId("0"),
                contextTime = 1000,
                contextSize = 0,
                contextNumMessages = 0,
                contextNumUnread = 0,
                contextNumAttachments = 0
            ),
            ConversationLabel(
                conversationId = conversationId,
                labelId = LabelId("1"),
                contextTime = 1000,
                contextSize = 0,
                contextNumMessages = 0,
                contextNumUnread = 0,
                contextNumAttachments = 0
            ),
            ConversationLabel(
                conversationId = conversationId,
                labelId = LabelId("2"),
                contextTime = 1000,
                contextSize = 0,
                contextNumMessages = 0,
                contextNumUnread = 0,
                contextNumAttachments = 0
            )
        )
    )

}
