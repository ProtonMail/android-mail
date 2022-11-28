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

package ch.protonmail.android.mailconversation.domain.entity

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import me.proton.core.label.domain.entity.LabelId

/**
 * @param conversationId the id of the conversation
 * @param labelId the id of the label
 * @param contextTime the time of the latest message within the conversation with this [labelId]
 * @param contextSize the sum of of all messages within the conversation with this [labelId]
 * @param contextNumMessages the number of all messages within the conversation with this [labelId]
 * @param contextNumUnread the number of all unread messages within the conversation with this [labelId]
 * @param contextNumAttachments the number of all attachments within the conversation with this [labelId]
 */
data class ConversationLabel(
    val conversationId: ConversationId,
    val labelId: LabelId,
    val contextTime: Long,
    val contextSize: Long,
    val contextNumMessages: Int,
    val contextNumUnread: Int,
    val contextNumAttachments: Int
)
