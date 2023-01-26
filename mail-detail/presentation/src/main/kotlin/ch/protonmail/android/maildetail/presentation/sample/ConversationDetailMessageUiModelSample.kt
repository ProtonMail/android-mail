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



package ch.protonmail.android.maildetail.presentation.sample

import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.domain.model.MessageWithLabels
import ch.protonmail.android.maildetail.domain.sample.MessageWithLabelsSample
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.mailmessage.domain.entity.Message

object ConversationDetailMessageUiModelSample {

    val AugWeatherForecast = build(
        messageWithLabels = MessageWithLabelsSample.AugWeatherForecast
    ).collapse()

    val EmptyDraft = build(
        messageWithLabels = MessageWithLabelsSample.EmptyDraft,
        avatar = AvatarUiModel.DraftIcon
    ).collapse()

    val ExpiringInvitation = build(
        messageWithLabels = MessageWithLabelsSample.ExpiringInvitation,
        expiration = TextUiModel("12h")
    ).collapse()

    val InvoiceForwarded = build(
        messageWithLabels = MessageWithLabelsSample.Invoice,
        forwardedIcon = ConversationDetailMessageUiModel.ForwardedIcon.Forwarded
    ).collapse()

    val InvoiceReplied = build(
        messageWithLabels = MessageWithLabelsSample.Invoice,
        repliedIcon = ConversationDetailMessageUiModel.RepliedIcon.Replied
    ).collapse()

    val InvoiceRepliedAll = build(
        messageWithLabels = MessageWithLabelsSample.Invoice,
        repliedIcon = ConversationDetailMessageUiModel.RepliedIcon.RepliedAll
    ).collapse()

    val LotteryScam = build(
        messageWithLabels = MessageWithLabelsSample.LotteryScam
    ).collapse()

    val SepWeatherForecast = build(
        messageWithLabels = MessageWithLabelsSample.SepWeatherForecast
    ).collapse()

    val StarredInvoice = build(
        messageWithLabels = MessageWithLabelsSample.Invoice,
        message = MessageWithLabelsSample.Invoice.message,
        isStarred = true
    ).collapse()

    val UnreadInvoice = build(
        messageWithLabels = MessageWithLabelsSample.UnreadInvoice
    ).collapse()

    private fun build(
        messageWithLabels: MessageWithLabels = MessageWithLabelsSample.build(),
        message: Message = messageWithLabels.message,
        avatar: AvatarUiModel = AvatarUiModel.ParticipantInitial(message.sender.name.substring(0, 1)),
        expiration: TextUiModel? = null,
        forwardedIcon: ConversationDetailMessageUiModel.ForwardedIcon =
            ConversationDetailMessageUiModel.ForwardedIcon.None,
        repliedIcon: ConversationDetailMessageUiModel.RepliedIcon = ConversationDetailMessageUiModel.RepliedIcon.None,
        isStarred: Boolean = false
    ): ConversationDetailMessageUiModel = ConversationDetailMessageUiModel.Collapsed(
        avatar = avatar,
        expiration = expiration,
        forwardedIcon = forwardedIcon,
        hasAttachments = message.numAttachments > message.attachmentCount.calendar,
        isStarred = isStarred,
        isUnread = message.unread,
        locationIcon = MessageLocationUiModelSample.AllMail,
        repliedIcon = repliedIcon,
        sender = message.sender.name,
        shortTime = TextUiModel("10:00"),
        labels = emptyList()
    )

    fun ConversationDetailMessageUiModel.expand() = ConversationDetailMessageUiModel.Expanded(
        avatar = avatar,
        expiration = expiration,
        forwardedIcon = forwardedIcon,
        hasAttachments = hasAttachments,
        isStarred = isStarred,
        isUnread = isUnread,
        locationIcon = locationIcon,
        repliedIcon = repliedIcon,
        sender = sender,
        shortTime = shortTime,
        labels = emptyList()
    )

    fun ConversationDetailMessageUiModel.collapse() = ConversationDetailMessageUiModel.Collapsed(
        avatar = avatar,
        expiration = expiration,
        forwardedIcon = forwardedIcon,
        hasAttachments = hasAttachments,
        isStarred = isStarred,
        isUnread = isUnread,
        locationIcon = locationIcon,
        repliedIcon = repliedIcon,
        sender = sender,
        shortTime = shortTime,
        emptyList()
    )
}
