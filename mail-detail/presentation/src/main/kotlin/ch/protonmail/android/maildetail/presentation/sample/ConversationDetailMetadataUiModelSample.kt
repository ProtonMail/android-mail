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



package ch.protonmail.android.maildetail.presentation.sample

import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMetadataUiModel
import ch.protonmail.android.maillabel.domain.model.SystemLabelId

object ConversationDetailMetadataUiModelSample {

    val WeatherForecast = build(conversation = ConversationSample.WeatherForecast)

    fun build(conversation: Conversation = ConversationSample.build()) = ConversationDetailMetadataUiModel(
        conversationId = conversation.conversationId,
        isStarred = conversation.labels.any { it.labelId == SystemLabelId.Starred.labelId },
        messageCount = conversation.numMessages,
        subject = conversation.subject
    )
}
