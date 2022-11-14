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
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.sample.MessageSample

object ConversationDetailMessageUiModelSample {

    val AugWeatherForecast = build(
        message = MessageSample.AugWeatherForecast
    ).collapse()

    val EmptyDraft = build(
        message = MessageSample.EmptyDraft,
        avatar = AvatarUiModel.DraftIcon
    ).collapse()

    val ExpiringInvitation = build(
        message = MessageSample.ExpiringInvitation,
        expiration = TextUiModel("12h")
    ).collapse()

    val InvoiceForwarded = build(
        message = MessageSample.Invoice,
        forwardedIcon = ConversationDetailMessageUiModel.ForwardedIcon.Forwarded
    ).collapse()

    val InvoiceReplied = build(
        message = MessageSample.Invoice,
        repliedIcon = ConversationDetailMessageUiModel.RepliedIcon.Replied
    ).collapse()

    val InvoiceRepliedAll = build(
        message = MessageSample.Invoice,
        repliedIcon = ConversationDetailMessageUiModel.RepliedIcon.RepliedAll
    ).collapse()

    val SepWeatherForecast = build(
        message = MessageSample.SepWeatherForecast
    ).collapse()

    val StarredInvoice = build(
        message = MessageSample.Invoice,
        isStarred = true
    ).collapse()

    private fun build(
        message: Message = MessageSample.build(),
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
        shortTime = TextUiModel("10:00")
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
        shortTime = shortTime
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
        shortTime = shortTime
    )
}
