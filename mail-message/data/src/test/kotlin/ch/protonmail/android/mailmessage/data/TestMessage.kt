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

package ch.protonmail.android.mailmessage.data

import ch.protonmail.android.mailconversation.data.remote.resource.RecipientResource
import ch.protonmail.android.mailmessage.data.remote.resource.MessageResource
import me.proton.core.domain.entity.UserId

fun getMessageResource(
    id: String = "1",
    order: Long = 1000,
    time: Long = 1000,
    labelIds: List<String> = listOf("0"),
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
    flags = 0
)

fun getMessage(
    userId: UserId = UserId("1"),
    id: String = "1",
    order: Long = 1000,
    time: Long = 1000,
    labelIds: List<String> = listOf("0"),
) = getMessageResource(
    id = id,
    order = order,
    time = time,
    labelIds = labelIds,
).toMessage(userId)
