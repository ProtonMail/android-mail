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
        sender = RecipientSample.PreciWeather,
        subject = "August weather forecast",
        time = Aug2022
    )

    val EmptyDraft = build(
        subject = "",
        labelIds = listOf(LabelIdSample.AllDraft)
    )

    val ExpiringInvitation = build(
        attachmentCount = AttachmentCountSample.CalendarInvite,
        numAttachments = AttachmentCountSample.CalendarInvite.calendar,
        expirationTime = Aug2022
    )

    val Invoice = build(
        conversationId = ConversationIdSample.Invoices,
        messageId = MessageIdSample.Invoice,
        labelIds = listOf(LabelIdSample.Archive, LabelIdSample.Document),
        numAttachments = 1,
        subject = "Invoice"
    )

    val UnreadInvoice = build(
        conversationId = ConversationIdSample.Invoices,
        messageId = MessageIdSample.Invoice,
        labelIds = listOf(LabelIdSample.Archive, LabelIdSample.Document),
        numAttachments = 1,
        subject = "Invoice",
        unread = true
    )

    val LotteryScam = build(
        sender = RecipientSample.Scammer
    )

    val OctWeatherForecast = build(
        conversationId = ConversationIdSample.WeatherForecast,
        messageId = MessageIdSample.OctWeatherForecast,
        labelIds = listOf(LabelIdSample.Archive),
        sender = RecipientSample.PreciWeather,
        subject = "October weather forecast",
        time = Oct2022
    )

    val SepWeatherForecast = build(
        conversationId = ConversationIdSample.WeatherForecast,
        messageId = MessageIdSample.SepWeatherForecast,
        labelIds = listOf(LabelIdSample.Archive),
        sender = RecipientSample.PreciWeather,
        subject = "September weather forecast",
        time = Sep2022
    )

    val AlphaAppInfoRequest = build(
        conversationId = ConversationIdSample.AlphaAppFeedback,
        messageId = MessageIdSample.AlphaAppInfoRequest,
        labelIds = listOf(LabelIdSample.Inbox),
        sender = RecipientSample.John,
        subject = "Request for details on features to test",
        time = Jan2023
    )

    val AlphaAppQAReport = build(
        conversationId = ConversationIdSample.AlphaAppFeedback,
        messageId = MessageIdSample.AlphaAppQAReport,
        labelIds = listOf(LabelIdSample.Inbox),
        sender = RecipientSample.John,
        subject = "QA testing session findings",
        time = Feb2023
    )

    val AlphaAppArchivedFeedback = build(
        conversationId = ConversationIdSample.AlphaAppFeedback,
        messageId = MessageIdSample.AlphaAppQAReport,
        labelIds = listOf(LabelIdSample.Archive),
        sender = RecipientSample.Doe,
        subject = "Is this a known issue?",
        time = Feb2023
    )


    private val Aug2022 get() = 1_659_312_000L
    private val Oct2022 get() = 1_664_582_400L
    private val Sep2022 get() = 1_661_990_400L
    private val Jan2023 get() = 1_672_531_200L
    private val Feb2023 get() = 1_675_209_600L

    fun build(
        addressId: AddressId = AddressIdSample.Primary,
        attachmentCount: AttachmentCount = AttachmentCountSample.build(),
        conversationId: ConversationId = ConversationIdSample.build(),
        expirationTime: Long = 0,
        isReplied: Boolean = false,
        messageId: MessageId = MessageIdSample.build(),
        labelIds: List<LabelId> = listOf(LabelIdSample.build()),
        numAttachments: Int = 0,
        order: Long = messageId.id.first().code.toLong(),
        sender: Recipient = RecipientSample.John,
        subject: String = "subject",
        time: Long = 1000,
        toList: List<Recipient> = emptyList(),
        userId: UserId = UserIdSample.Primary,
        unread: Boolean = false
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
        isReplied = isReplied,
        isRepliedAll = false,
        labelIds = labelIds,
        messageId = messageId,
        numAttachments = numAttachments,
        order = order,
        sender = sender,
        size = 0,
        subject = subject,
        time = time,
        toList = toList,
        unread = unread,
        userId = userId
    )

    fun Message.moveTo(labelId: LabelId): Message = copy(
        labelIds = labelIds + labelId
    )

    fun Message.labelAs(labelIds: List<LabelId>): Message = copy(
        labelIds = labelIds
    )
}
