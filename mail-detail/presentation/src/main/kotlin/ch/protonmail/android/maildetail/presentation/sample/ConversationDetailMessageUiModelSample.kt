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
    )

    val EmptyDraft = build(
        message = MessageSample.EmptyDraft,
        avatar = AvatarUiModel.DraftIcon
    )

    val ExpiringInvitation = build(
        message = MessageSample.ExpiringInvitation,
        expiration = TextUiModel("12h")
    )

    val InvoiceForwarded = build(
        message = MessageSample.Invoice,
        forwardedIcon = ConversationDetailMessageUiModel.ForwardedIcon.Forwarded
    )

    val InvoiceReplied = build(
        message = MessageSample.Invoice,
        repliedIcon = ConversationDetailMessageUiModel.RepliedIcon.Replied
    )

    val InvoiceRepliedAll = build(
        message = MessageSample.Invoice,
        repliedIcon = ConversationDetailMessageUiModel.RepliedIcon.RepliedAll
    )

    val SepWeatherForecast = build(
        message = MessageSample.SepWeatherForecast
    )

    fun build(
        isExpanded: Boolean = false,
        @Suppress("UNUSED_PARAMETER") message: Message = MessageSample.build(),
        avatar: AvatarUiModel = AvatarUiModel.ParticipantInitial(message.sender.name.substring(0, 1)),
        expiration: TextUiModel? = null,
        forwardedIcon: ConversationDetailMessageUiModel.ForwardedIcon =
            ConversationDetailMessageUiModel.ForwardedIcon.None,
        repliedIcon: ConversationDetailMessageUiModel.RepliedIcon = ConversationDetailMessageUiModel.RepliedIcon.None
    ): ConversationDetailMessageUiModel = when {
        isExpanded -> ConversationDetailMessageUiModel.Expanded(
            avatar = avatar,
            expiration = expiration,
            forwardedIcon = forwardedIcon,
            isUnread = message.unread,
            repliedIcon = repliedIcon,
            sender = message.sender.name
        )
        else -> ConversationDetailMessageUiModel.Collapsed(
            avatar = avatar,
            expiration = expiration,
            forwardedIcon = forwardedIcon,
            isUnread = message.unread,
            repliedIcon = repliedIcon,
            sender = message.sender.name
        )
    }

    fun ConversationDetailMessageUiModel.expand() = ConversationDetailMessageUiModel.Expanded(
        avatar = avatar,
        expiration = expiration,
        forwardedIcon = forwardedIcon,
        isUnread = isUnread,
        repliedIcon = repliedIcon,
        sender = sender
    )

    fun ConversationDetailMessageUiModel.collapse() = ConversationDetailMessageUiModel.Collapsed(
        avatar = avatar,
        expiration = expiration,
        forwardedIcon = forwardedIcon,
        isUnread = isUnread,
        repliedIcon = repliedIcon,
        sender = sender
    )
}
