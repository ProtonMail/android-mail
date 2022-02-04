/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmailbox.domain

import ch.protonmail.android.mailconversation.domain.ConversationId
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.Sender
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.user.domain.entity.AddressId

fun getMessage(
    userId: UserId,
    id: String,
    order: Long = 1000,
    time: Long = 1000,
    labelIds: List<String> = listOf("0"),
) = Message(
    userId = userId,
    messageId = MessageId(id),
    conversationId = ConversationId(id),
    time = time,
    size = 1000,
    order = order,
    labelIds = labelIds.map { LabelId(it) },
    subject = "subject",
    unread = false,
    sender = Sender("address", "name"),
    toList = emptyList(),
    ccList = emptyList(),
    bccList = emptyList(),
    expirationTime = 1000,
    isReplied = false,
    isRepliedAll = false,
    isForwarded = false,
    addressId = AddressId("1"),
    externalId = null,
    numAttachments = 0,
    flags = 0
)
