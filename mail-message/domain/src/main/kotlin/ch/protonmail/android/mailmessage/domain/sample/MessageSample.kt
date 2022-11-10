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

package ch.protonmail.android.mailmessage.domain.sample

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.sample.AddressIdSample
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.entity.AttachmentCount
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.Recipient
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.user.domain.entity.AddressId

object MessageSample {

    val AugWeatherForecast = build(
        conversationId = ConversationIdSample.WeatherForecast,
        messageId = MessageIdSample.AugWeatherForecast,
        labelIds = listOf(LabelIdSample.Archive),
        subject = "August weather forecast",
        time = Aug2022
    )
    val Invoice = build(
        messageId = MessageIdSample.Invoice,
        labelIds = listOf(LabelIdSample.Archive, LabelIdSample.Document),
        subject = "Invoice"
    )
    val SepWeatherForecast = build(
        conversationId = ConversationIdSample.WeatherForecast,
        messageId = MessageIdSample.SepWeatherForecast,
        labelIds = listOf(LabelIdSample.Archive),
        subject = "September weather forecast",
        time = Sep2022
    )

    private val Aug2022 get() = 1_630_403_200_000L
    private val Sep2022 get() = 1_633_081_600_000L

    fun build(
        addressId: AddressId = AddressIdSample.Primary,
        attachmentCount: AttachmentCount = AttachmentCountSample.build(),
        conversationId: ConversationId = ConversationIdSample.build(),
        expirationTime: Long = 0,
        messageId: MessageId = MessageIdSample.build(),
        labelIds: List<LabelId> = listOf(LabelIdSample.build()),
        numAttachments: Int = 0,
        order: Long = messageId.id.first().code.toLong(),
        sender: Recipient = RecipientSample.John,
        subject: String = "subject",
        time: Long = 1000,
        userId: UserId = UserIdSample.Primary
    ) = Message(
        addressId = addressId,
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
        sender = sender,
        size = 0,
        subject = subject,
        time = time,
        toList = emptyList(),
        unread = false,
        userId = userId
    )
}
