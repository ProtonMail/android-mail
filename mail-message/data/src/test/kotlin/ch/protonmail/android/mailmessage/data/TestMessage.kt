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

package ch.protonmail.android.mailmessage.data

import ch.protonmail.android.mailmessage.data.local.relation.MessageWithLabelIds
import ch.protonmail.android.mailmessage.data.local.toEntity
import ch.protonmail.android.mailmessage.data.remote.resource.AttachmentCountsResource
import ch.protonmail.android.mailmessage.data.remote.resource.AttachmentsInfoResource
import ch.protonmail.android.mailmessage.data.remote.resource.MessageResource
import ch.protonmail.android.mailmessage.data.remote.resource.RecipientResource
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

fun getMessageResource(
    id: String = "1",
    order: Long = 1000,
    time: Long = 1000,
    labelIds: List<String> = listOf("0")
) = MessageResource(
    id = id,
    order = order,
    conversationId = "1",
    subject = "subject",
    unread = 0,
    sender = RecipientResource(
        address = "email@domain.com",
        name = "name"
    ),
    toList = emptyList(),
    ccList = emptyList(),
    bccList = emptyList(),
    time = time,
    size = 1000,
    expirationTime = 1000,
    isReplied = 0,
    isRepliedAll = 0,
    isForwarded = 0,
    addressId = "1",
    labelIds = labelIds,
    externalId = null,
    numAttachments = 0,
    flags = 0,
    attachmentsInfo = AttachmentsInfoResource(
        ics = AttachmentCountsResource(attachedCount = 0)
    )
)

fun getMessage(
    userId: UserId = UserId("1"),
    id: String = "1",
    order: Long = 1000,
    time: Long = 1000,
    labelIds: List<String> = listOf("0")
) = getMessageResource(
    id = id,
    order = order,
    time = time,
    labelIds = labelIds
).toMessage(userId)

fun getMessageWithLabels(
    userId: UserId = UserId("1"),
    id: String = "1",
    labelIds: List<LabelId> = listOf(LabelId("0"))
) = MessageWithLabelIds(
    getMessageResource(id).toMessage(userId).toEntity(),
    labelIds
)
