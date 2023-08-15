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

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailmessage.data.local.entity.AttachmentCountEntity
import ch.protonmail.android.mailmessage.data.local.entity.MessageEntity
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.model.Sender
import ch.protonmail.android.testdata.user.UserIdTestData
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId

object MessageEntityTestData {

    val messageEntity = buildMessageEntity(id = MessageTestData.RAW_MESSAGE_ID, subject = MessageTestData.RAW_SUBJECT)

    private fun buildMessageEntity(
        userId: UserId = UserIdTestData.userId,
        id: String,
        order: Long = 1000,
        time: Long = 1000,
        size: Long = 0,
        subject: String = "subject",
        sender: Sender = Sender("address", "name"),
        numAttachments: Int = 0,
        expirationTime: Long = 0,
        attachmentCount: AttachmentCountEntity = AttachmentCountEntity(0),
        toList: List<Recipient> = emptyList(),
        ccList: List<Recipient> = emptyList(),
        bccList: List<Recipient> = emptyList(),
        conversationId: ConversationId = ConversationId(id)
    ) = MessageEntity(
        userId = userId,
        messageId = MessageId(id),
        conversationId = conversationId,
        time = time,
        size = size,
        order = order,
        subject = subject,
        unread = false,
        sender = sender,
        toList = toList,
        ccList = ccList,
        bccList = bccList,
        expirationTime = expirationTime,
        isReplied = false,
        isRepliedAll = false,
        isForwarded = false,
        addressId = AddressId("1"),
        externalId = null,
        numAttachments = numAttachments,
        flags = 0,
        attachmentCount = attachmentCount
    )
}
