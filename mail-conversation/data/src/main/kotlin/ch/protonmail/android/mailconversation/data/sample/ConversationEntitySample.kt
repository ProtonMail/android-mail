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

package ch.protonmail.android.mailconversation.data.sample

import ch.protonmail.android.mailconversation.data.local.entity.ConversationEntity
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.mailmessage.data.sample.AttachmentCountEntitySample

object ConversationEntitySample {

    val WeatherForecast: ConversationEntity = build(ConversationSample.WeatherForecast)
    val AlphaAppFeedback: ConversationEntity = build(ConversationSample.AlphaAppFeedback)

    fun build(conversation: Conversation = ConversationSample.build()) = ConversationEntity(
        attachmentCount = AttachmentCountEntitySample.build(),
        conversationId = conversation.conversationId,
        expirationTime = conversation.expirationTime,
        numAttachments = conversation.numAttachments,
        numMessages = conversation.numMessages,
        numUnread = conversation.numUnread,
        order = conversation.order,
        recipients = conversation.recipients,
        senders = conversation.senders,
        subject = conversation.subject,
        userId = conversation.userId
    )
}
