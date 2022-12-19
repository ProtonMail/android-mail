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

sealed interface ConversationDetailMessageUiModel {

    val avatar: AvatarUiModel
    val expiration: TextUiModel?
    val forwardedIcon: ForwardedIcon
    val hasAttachments: Boolean
    val isStarred: Boolean
    val isUnread: Boolean
    val locationIcon: MessageLocationUiModel
    val repliedIcon: RepliedIcon
    val sender: String
    val shortTime: TextUiModel
    val labels: List<LabelUiModel>

    data class Collapsed(
        override val avatar: AvatarUiModel,
        override val expiration: TextUiModel?,
        override val forwardedIcon: ForwardedIcon,
        override val hasAttachments: Boolean,
        override val isStarred: Boolean,
        override val isUnread: Boolean,
        override val locationIcon: MessageLocationUiModel,
        override val repliedIcon: RepliedIcon,
        override val sender: String,
        override val shortTime: TextUiModel,
        override val labels: List<LabelUiModel>
    ) : ConversationDetailMessageUiModel

    data class Expanded(
        override val avatar: AvatarUiModel,
        override val expiration: TextUiModel?,
        override val forwardedIcon: ForwardedIcon,
        override val hasAttachments: Boolean,
        override val isStarred: Boolean,
        override val isUnread: Boolean,
        override val locationIcon: MessageLocationUiModel,
        override val repliedIcon: RepliedIcon,
        override val sender: String,
        override val shortTime: TextUiModel,
        override val labels: List<LabelUiModel>
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
