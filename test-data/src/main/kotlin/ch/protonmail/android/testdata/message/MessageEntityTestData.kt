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

package ch.protonmail.android.testdata.message

import ch.protonmail.android.mailmessage.data.local.entity.MessageEntity
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.testdata.attachment.AttachmentCountEntityTestData
import ch.protonmail.android.testdata.user.UserIdTestData

object MessageEntityTestData {

    val AugWeatherForecast = build(MessageTestData.AugWeatherForecast)
    val Invoice = build(MessageTestData.Invoice)
    val SepWeatherForecast = build(MessageTestData.SepWeatherForecast)

    fun build(message: Message = MessageTestData.build()) = MessageEntity(
        addressId = message.addressId,
        attachmentCount = AttachmentCountEntityTestData.build(),
        bccList = message.bccList,
        ccList = message.ccList,
        conversationId = message.conversationId,
        expirationTime = message.expirationTime,
        externalId = message.externalId,
        flags = message.flags,
        isForwarded = message.isForwarded,
        isReplied = message.isReplied,
        isRepliedAll = message.isRepliedAll,
        messageId = message.messageId,
        numAttachments = message.numAttachments,
        order = message.order,
        sender = message.sender,
        size = message.size,
        subject = message.subject,
        time = message.time,
        toList = message.toList,
        unread = message.unread,
        userId = UserIdTestData.Primary
    )
}
