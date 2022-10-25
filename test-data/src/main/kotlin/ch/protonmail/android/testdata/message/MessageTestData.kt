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
import ch.protonmail.android.mailmessage.domain.entity.AttachmentCount
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.Recipient
import ch.protonmail.android.testdata.address.AddressIdTestData
import ch.protonmail.android.testdata.conversation.ConversationIdTestData
import ch.protonmail.android.testdata.label.LabelIdTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.user.domain.entity.AddressId

object MessageTestData {

    const val RAW_MESSAGE_ID = "rawMessageId"
    const val RAW_SUBJECT = "Here's a new message"

    val AugWeatherForecast = build(
        conversationId = ConversationIdTestData.WeatherForecast,
        messageId = MessageIdTestData.AugWeatherForecast,
        labelIds = listOf(LabelIdTestData.Archive),
        time = Aug2022
    )
    val Invoice = build(
        messageId = MessageIdTestData.Invoice,
        labelIds = listOf(LabelIdTestData.Archive, LabelIdTestData.Document)
    )
    val SepWeatherForecast = build(
        conversationId = ConversationIdTestData.WeatherForecast,
        messageId = MessageIdTestData.SepWeatherForecast,
        labelIds = listOf(LabelIdTestData.Archive),
        time = Sep2022
    )

    val message = buildMessage(
        userId = userId,
        id = RAW_MESSAGE_ID,
        subject = RAW_SUBJECT,
        labelIds = listOf("0")
    )

    private val Aug2022 get() = 1_630_403_200_000L
    private val Sep2022 get() = 1_633_081_600_000L

    fun build(
        attachmentCount: AttachmentCount = AttachmentCount(0),
        conversationId: ConversationId = ConversationIdTestData.build(),
        expirationTime: Long = 0,
        messageId: MessageId = MessageId("message1"),
        labelIds: List<LabelId> = emptyList(),
        numAttachments: Int = 0,
        order: Long = messageId.id.first().code.toLong(),
        subject: String = "subject",
        time: Long = 1000,
        userId: UserId = UserIdTestData.Primary
    ) = Message(
        addressId = AddressIdTestData.Primary,
        attachmentCount = attachmentCount,
        bccList = emptyList(),
        ccList = emptyList(),
        conversationId = conversationId,
        expirationTime = expirationTime,
        externalId = null,
        flags = 0,
        isForwarded = false,
        isReplied = false,
        isRepliedAll = false,
        labelIds = labelIds,
        messageId = messageId,
        numAttachments = numAttachments,
        order = order,
        sender = Recipient("address", "name"),
        size = 0,
        subject = subject,
        time = time,
        toList = emptyList(),
        unread = false,
        userId = userId
    )

    fun buildMessage(
        userId: UserId = UserIdTestData.userId,
        id: String,
        order: Long = 1000,
        time: Long = 1000,
        labelIds: List<String> = listOf("0"),
        subject: String = "subject",
        numAttachments: Int = 0,
        expirationTime: Long = 0,
        attachmentCount: AttachmentCount = AttachmentCount(0)
    ) = Message(
        userId = userId,
        messageId = MessageId(id),
        conversationId = ConversationId(id),
        time = time,
        size = 0,
        order = order,
        labelIds = labelIds.map { LabelId(it) },
        subject = subject,
        unread = false,
        sender = Recipient("address", "name"),
        toList = emptyList(),
        ccList = emptyList(),
        bccList = emptyList(),
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
