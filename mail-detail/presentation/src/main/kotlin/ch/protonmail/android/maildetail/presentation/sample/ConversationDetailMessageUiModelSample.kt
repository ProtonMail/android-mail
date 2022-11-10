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

import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.sample.MessageSample

object ConversationDetailMessageUiModelSample {

    val AugWeatherForecast = build(message = MessageSample.AugWeatherForecast)
    val SepWeatherForecast = build(message = MessageSample.SepWeatherForecast)

    fun build(
        isExpanded: Boolean = false,
        @Suppress("UNUSED_PARAMETER") message: Message = MessageSample.build()
    ): ConversationDetailMessageUiModel = when {
        isExpanded -> ConversationDetailMessageUiModel.Expanded(
            isUnread = message.unread,
            subject = message.subject
        )
        else -> ConversationDetailMessageUiModel.Collapsed(
            isUnread = message.unread,
            subject = message.subject
        )
    }

    fun ConversationDetailMessageUiModel.Collapsed.expand() = ConversationDetailMessageUiModel.Expanded(
        isUnread = isUnread,
        subject = subject
    )

    fun ConversationDetailMessageUiModel.Expanded.collapse() = ConversationDetailMessageUiModel.Collapsed(
        isUnread = isUnread,
        subject = subject
    )
}
