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

package ch.protonmail.android.mailmessage.data.local

import ch.protonmail.android.mailmessage.data.local.entity.MessageEntity
import ch.protonmail.android.mailmessage.data.local.relation.MessageWithLabelIds
import ch.protonmail.android.mailmessage.data.mapper.toDomainModel
import ch.protonmail.android.mailmessage.data.mapper.toEntity
import ch.protonmail.android.mailmessage.domain.model.Message

fun MessageWithLabelIds.toMessage() = Message(
    userId = message.userId,
    messageId = message.messageId,
    conversationId = message.conversationId,
    time = message.time,
    size = message.size,
    order = message.order,
    labelIds = labelIds,
    subject = message.subject,
    unread = message.unread,
    sender = message.sender,
    toList = message.toList,
    ccList = message.ccList,
    bccList = message.bccList,
    expirationTime = message.expirationTime,
    isReplied = message.isReplied,
    isRepliedAll = message.isRepliedAll,
    isForwarded = message.isForwarded,
    addressId = message.addressId,
    externalId = message.externalId,
    numAttachments = message.numAttachments,
    flags = message.flags,
    attachmentCount = message.attachmentCount.toDomainModel()
)

fun Message.toEntity() = MessageEntity(
    userId = userId,
    messageId = messageId,
    conversationId = conversationId,
    order = order,
    subject = subject,
    unread = unread,
    sender = sender,
    toList = toList,
    ccList = ccList,
    bccList = bccList,
    time = time,
    size = size,
    expirationTime = expirationTime,
    isReplied = isReplied,
    isRepliedAll = isRepliedAll,
    isForwarded = isForwarded,
    addressId = addressId,
    externalId = externalId,
    numAttachments = numAttachments,
    flags = flags,
    attachmentCount = attachmentCount.toEntity()
)
