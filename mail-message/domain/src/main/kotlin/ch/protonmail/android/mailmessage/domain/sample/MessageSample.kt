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

import ch.protonmail.android.mailattachments.domain.model.AttachmentCount
import ch.protonmail.android.mailattachments.domain.sample.AttachmentCountSample
import ch.protonmail.android.mailattachments.domain.sample.AttachmentMetadataSamples
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.sample.AddressIdSample
import ch.protonmail.android.mailcommon.domain.sample.AvatarInformationSample
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maillabel.domain.model.CategoryLocation
import ch.protonmail.android.maillabel.domain.model.ExclusiveLocation
import ch.protonmail.android.maillabel.domain.model.Label
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.sample.LabelSample
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.model.Sender
import ch.protonmail.android.mailsnooze.domain.model.NoSnooze
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId

object MessageSample {

    val AugWeatherForecast = build(
        conversationId = ConversationIdSample.WeatherForecast,
        messageId = MessageIdSample.AugWeatherForecast,
        sender = RecipientSample.PreciWeather,
        subject = "August weather forecast",
        time = Aug2022
    )

    val AugWeatherForecastFolder2021 = build(
        conversationId = ConversationIdSample.WeatherForecast,
        messageId = MessageIdSample.AugWeatherForecast,
        sender = RecipientSample.PreciWeather,
        subject = "August weather forecast",
        time = Aug2022
    )

    val EmptyDraft = build(
        subject = ""
    )

    val NewDraftWithSubject = build(
        messageId = MessageIdSample.NewDraftWithSubject,
        sender = RecipientSample.John,
        subject = "New draft, just typed the subject"
    )

    val NewDraftWithSubjectAndBody = build(
        messageId = MessageIdSample.NewDraftWithSubjectAndBody,
        sender = RecipientSample.John,
        subject = "New draft, just typed the subject and the body"
    )

    val RemoteDraft = build(
        messageId = MessageIdSample.RemoteDraft,
        sender = RecipientSample.John,
        subject = "Remote draft, known to the API"
    )

    val RemoteDraftWith4RecipientTypes = build(
        messageId = MessageIdSample.RemoteDraft,
        sender = RecipientSample.John,
        subject = "Remote draft, known to the API, 4 recipients total in TO, CC and BCC",
        toList = listOf(RecipientSample.Doe),
        ccList = listOf(RecipientSample.PreciWeather),
        bccList = listOf(RecipientSample.Scammer, RecipientSample.ExternalEncrypted)
    )

    val ExpiringInvitation = build(
        attachmentCount = AttachmentCountSample.CalendarInvite,
        expirationTime = Aug2022,
        numAttachments = AttachmentCountSample.CalendarInvite.calendar
    )

    val HtmlInvoice = build(
        conversationId = ConversationIdSample.Invoices,
        messageId = MessageIdSample.HtmlInvoice,
        numAttachments = 0,
        subject = "Invoice in html format"
    )

    val Invoice = build(
        conversationId = ConversationIdSample.Invoices,
        messageId = MessageIdSample.Invoice,
        numAttachments = 1,
        subject = "Invoice"
    )

    val InvoiceWithTwoLabels = build(
        conversationId = ConversationIdSample.Invoices,
        messageId = MessageIdSample.Invoice,
        numAttachments = 1,
        subject = "Invoice",
        customLabels = listOf(
            LabelSample.Label2021,
            LabelSample.Label2022
        )
    )

    val UnreadInvoice = build(
        conversationId = ConversationIdSample.Invoices,
        messageId = MessageIdSample.Invoice,
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
        sender = RecipientSample.PreciWeather,
        subject = "October weather forecast",
        time = Oct2022
    )

    val SepWeatherForecast = build(
        conversationId = ConversationIdSample.WeatherForecast,
        messageId = MessageIdSample.SepWeatherForecast,
        sender = RecipientSample.PreciWeather,
        subject = "September weather forecast",
        time = Sep2022
    )

    val AlphaAppInfoRequest = build(
        conversationId = ConversationIdSample.AlphaAppFeedback,
        messageId = MessageIdSample.AlphaAppInfoRequest,
        sender = RecipientSample.John,
        subject = "Request for details on features to test",
        time = Jan2023
    )

    val AlphaAppQAReport = build(
        conversationId = ConversationIdSample.AlphaAppFeedback,
        messageId = MessageIdSample.AlphaAppQAReport,
        sender = RecipientSample.John,
        subject = "QA testing session findings",
        time = Feb2023
    )

    val AlphaAppArchivedFeedback = build(
        conversationId = ConversationIdSample.AlphaAppFeedback,
        messageId = MessageIdSample.AlphaAppQAReport,
        sender = RecipientSample.Doe,
        subject = "Is this a known issue?",
        time = Feb2023
    )

    val MessageWithAttachments = build(
        conversationId = ConversationIdSample.Invoices,
        messageId = MessageIdSample.MessageWithAttachments,
        numAttachments = 3,
        subject = "Sending some documents"
    )

    val PgpMimeMessage = build(
        messageId = MessageIdSample.PgpMimeMessage
    )

    val CalendarInvite = build(
        messageId = MessageIdSample.CalendarInvite,
        numAttachments = 1,
        sender = RecipientSample.John,
        subject = "Calendar invite",
        toList = listOf(RecipientSample.Bob)
    )

    val ReadMessageMayFirst = build(
        messageId = MessageIdSample.ReadMessageMayFirst,
        time = MayFirst2023,
        unread = false
    )

    val ReadMessageMaySecond = build(
        messageId = MessageIdSample.ReadMessageMaySecond,
        time = MaySecond2023,
        unread = false
    )

    val ReadMessageMayThird = build(
        messageId = MessageIdSample.UnreadMessageMayThird,
        time = MayThird2023,
        unread = false
    )

    val UnreadMessageMayFirst = build(
        messageId = MessageIdSample.UnreadMessageMayFirst,
        time = MayFirst2023,
        unread = true
    )

    val UnreadMessageMaySecond = build(
        messageId = MessageIdSample.UnreadMessageMaySecond,
        time = MaySecond2023,
        unread = true
    )

    val UnreadMessageMayThird = build(
        messageId = MessageIdSample.UnreadMessageMayThird,
        time = MayThird2023,
        unread = true
    )

    val MessageWithNoExclusiveLocation = build(
        conversationId = ConversationIdSample.AlphaAppFeedback,
        messageId = MessageIdSample.AlphaAppQAReport,
        sender = RecipientSample.John,
        subject = "This message is orphan...",
        time = Oct2022,
        exclusiveLocation = ExclusiveLocation.NoLocation
    )

    private val MayFirst2023 get() = 1_682_899_200L
    private val MaySecond2023 get() = 1_682_985_600L
    private val MayThird2023 get() = 1_683_072_000L
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
        numAttachments: Int = 0,
        order: Long = messageId.id.first().code.toLong(),
        sender: Sender = RecipientSample.John,
        subject: String = "subject",
        time: Long = 1000,
        toList: List<Recipient> = emptyList(),
        ccList: List<Recipient> = emptyList(),
        bccList: List<Recipient> = emptyList(),
        userId: UserId = UserIdSample.Primary,
        unread: Boolean = false,
        customLabels: List<Label> = emptyList(),
        exclusiveLocation: ExclusiveLocation = ExclusiveLocation.System(SystemLabelId.Inbox, LabelId("1")),
        category: CategoryLocation = CategoryLocation.NoValue
    ) = Message(
        messageId = messageId,
        conversationId = conversationId,
        time = time,
        size = 0,
        order = order,
        subject = subject,
        isUnread = unread,
        sender = sender,
        toList = toList,
        ccList = ccList,
        bccList = bccList,
        expirationTime = expirationTime,
        isReplied = isReplied,
        isRepliedAll = false,
        isForwarded = false,
        isStarred = false,
        addressId = addressId,
        numAttachments = numAttachments,
        flags = 0,
        attachmentCount = attachmentCount,
        attachmentPreviews = listOf(AttachmentMetadataSamples.Zip),
        customLabels = customLabels,
        avatarInformation = AvatarInformationSample.avatarSample,
        exclusiveLocation = exclusiveLocation,
        category = category,
        isDraft = false,
        isScheduled = false,
        isReplyAllowed = true,
        snoozeInformation = NoSnooze
    )
}
