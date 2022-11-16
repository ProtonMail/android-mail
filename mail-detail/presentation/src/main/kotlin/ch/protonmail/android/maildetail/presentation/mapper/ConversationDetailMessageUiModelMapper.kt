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

package ch.protonmail.android.maildetail.presentation.mapper

import ch.protonmail.android.mailcommon.presentation.usecase.FormatExpiration
import ch.protonmail.android.mailcommon.presentation.usecase.FormatShortTime
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import me.proton.core.contact.domain.entity.Contact
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class ConversationDetailMessageUiModelMapper @Inject constructor(
    private val avatarUiModelMapper: DetailAvatarUiModelMapper,
    private val formatExpiration: FormatExpiration,
    private val formatShortTime: FormatShortTime,
    private val messageLocationUiModelMapper: MessageLocationUiModelMapper,
    private val resolveParticipantName: ResolveParticipantName
) {

    fun toUiModel(message: Message, contacts: List<Contact>): ConversationDetailMessageUiModel {
        val senderResolvedName = resolveParticipantName(message.sender, contacts)
        return ConversationDetailMessageUiModel.Collapsed(
            avatar = avatarUiModelMapper(
                message,
                senderResolvedName = senderResolvedName
            ),
            expiration = message.expirationTime.takeIf { it > 0 }?.let { formatExpiration(it.seconds) },
            forwardedIcon = getForwardedIcon(isForwarded = message.isForwarded),
            hasAttachments = message.numAttachments > message.attachmentCount.calendar,
            isStarred = SystemLabelId.Starred.labelId in message.labelIds,
            isUnread = message.unread,
            locationIcon = messageLocationUiModelMapper(message.labelIds, TODO()),
            repliedIcon = getRepliedIcon(isReplied = message.isReplied, isRepliedAll = message.isRepliedAll),
            sender = senderResolvedName,
            shortTime = formatShortTime(message.time.seconds)
        )
    }

    private fun getForwardedIcon(
        isForwarded: Boolean
    ): ConversationDetailMessageUiModel.ForwardedIcon = when {
        isForwarded -> ConversationDetailMessageUiModel.ForwardedIcon.Forwarded
        else -> ConversationDetailMessageUiModel.ForwardedIcon.None
    }

    private fun getRepliedIcon(
        isReplied: Boolean,
        isRepliedAll: Boolean
    ): ConversationDetailMessageUiModel.RepliedIcon = when {
        isRepliedAll -> ConversationDetailMessageUiModel.RepliedIcon.RepliedAll
        isReplied -> ConversationDetailMessageUiModel.RepliedIcon.Replied
        else -> ConversationDetailMessageUiModel.RepliedIcon.None
    }
}

