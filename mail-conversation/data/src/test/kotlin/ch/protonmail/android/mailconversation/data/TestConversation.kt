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

package ch.protonmail.android.mailconversation.data

import ch.protonmail.android.mailconversation.data.local.relation.ConversationWithLabels
import ch.protonmail.android.mailconversation.data.local.toEntity
import ch.protonmail.android.mailconversation.data.remote.resource.ConversationLabelResource
import ch.protonmail.android.mailconversation.data.remote.resource.ConversationResource
import ch.protonmail.android.mailconversation.domain.entity.ConversationLabel
import ch.protonmail.android.mailmessage.data.remote.resource.AttachmentCountsResource
import ch.protonmail.android.mailmessage.data.remote.resource.AttachmentsInfoResource
import me.proton.core.domain.entity.UserId

fun getConversationResource(
    id: String = "1",
    order: Long = 1000,
    labels: List<ConversationLabelResource> = listOf(getConversationLabelResource(id))
) = ConversationResource(
    id = id,
    order = order,
    subject = "subject",
    expirationTime = 0,
    labels = labels,
    numAttachments = 2,
    numMessages = 1,
    numUnread = 0,
    recipients = emptyList(),
    senders = emptyList(),
    attachmentsInfo = AttachmentsInfoResource(
        applicationIcs = AttachmentCountsResource(attachedCount = 0)
    )
)

fun getConversationLabelResource(id: String, contextTime: Long = 1000) = ConversationLabelResource(
    id = id,
    contextNumUnread = 0,
    contextNumMessages = 0,
    contextTime = contextTime,
    contextSize = 0,
    contextNumAttachments = 0
)

fun getConversation(
    userId: UserId = UserId("1"),
    id: String = "1",
    order: Long = 1000,
    time: Long = 1000,
    labelIds: List<String> = listOf("0")
) = getConversationResource(
    id = id,
    order = order,
    labels = labelIds.map { getConversationLabelResource(it, contextTime = time) }
).toConversation(userId)

fun getConversationWithLabels(
    userId: UserId = UserId("1"),
    id: String = "1",
    labels: List<ConversationLabel> = listOf()
) = ConversationWithLabels(
    getConversationResource(id).toConversation(userId).toEntity(),
    labels
)
