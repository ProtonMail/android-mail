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

import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.presentation.model.LabelUiModel
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import kotlinx.collections.immutable.ImmutableList
import me.proton.core.user.domain.entity.UserAddress

sealed interface ConversationDetailMessageUiModel {

    val messageId: MessageIdUiModel
    val isUnread: Boolean

    data class Hidden(
        override val messageId: MessageIdUiModel,
        override val isUnread: Boolean
    ) : ConversationDetailMessageUiModel

    data class Collapsed(
        override val messageId: MessageIdUiModel,
        val avatar: AvatarUiModel,
        val expiration: TextUiModel?,
        val forwardedIcon: ForwardedIcon,
        val hasAttachments: Boolean,
        val isStarred: Boolean,
        override val isUnread: Boolean,
        val locationIcon: MessageLocationUiModel,
        val repliedIcon: RepliedIcon,
        val sender: ParticipantUiModel,
        val shortTime: TextUiModel,
        val labels: ImmutableList<LabelUiModel>,
        val isDraft: Boolean
    ) : ConversationDetailMessageUiModel

    data class Expanding(
        override val messageId: MessageIdUiModel,
        val collapsed: Collapsed,
        override val isUnread: Boolean = collapsed.isUnread
    ) : ConversationDetailMessageUiModel

    data class Expanded(
        override val messageId: MessageIdUiModel,
        override val isUnread: Boolean,
        val messageDetailHeaderUiModel: MessageDetailHeaderUiModel,
        val messageDetailFooterUiModel: MessageDetailFooterUiModel,
        val messageBannersUiModel: MessageBannersUiModel,
        val messageBodyUiModel: MessageBodyUiModel,
        val requestPhishingLinkConfirmation: Boolean,
        val expandCollapseMode: MessageBodyExpandCollapseMode,
        val userAddress: UserAddress
    ) : ConversationDetailMessageUiModel

    enum class ForwardedIcon {
        None,
        Forwarded
    }

    enum class RepliedIcon {
        None,
        Replied,
        RepliedAll
    }
}
