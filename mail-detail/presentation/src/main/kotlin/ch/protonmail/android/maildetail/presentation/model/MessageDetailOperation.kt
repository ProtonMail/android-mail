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

package ch.protonmail.android.maildetail.presentation.model

import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody

sealed interface MessageDetailOperation {
    sealed interface AffectingMessage
}

sealed interface MessageDetailEvent : MessageDetailOperation {

    data class MessageMetadata(val messageUiModel: MessageDetailMetadataUiModel) :
        MessageDetailEvent,
        MessageDetailOperation.AffectingMessage

    data class MessageBody(val message: MessageWithBody) :
        MessageDetailEvent,
        MessageDetailOperation.AffectingMessage

    object NoPrimaryUser : MessageDetailEvent, MessageDetailOperation.AffectingMessage
    object NoCachedMetadata : MessageDetailEvent, MessageDetailOperation.AffectingMessage

    data class MessageBottomBarEvent(val bottomBarEvent: BottomBarEvent) : MessageDetailEvent
}

sealed interface MessageViewAction : MessageDetailOperation {
    object Star : MessageViewAction, MessageDetailOperation.AffectingMessage
    object UnStar : MessageViewAction, MessageDetailOperation.AffectingMessage
    object MarkUnread : MessageViewAction
}
