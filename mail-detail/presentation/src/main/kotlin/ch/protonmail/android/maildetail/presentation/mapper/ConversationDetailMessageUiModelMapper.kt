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

import androidx.compose.ui.graphics.Color
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcommon.presentation.mapper.ExpirationTimeMapper
import ch.protonmail.android.mailcommon.presentation.usecase.FormatShortTime
import ch.protonmail.android.maildetail.domain.model.DecryptedMessageBody
import ch.protonmail.android.maildetail.domain.model.MessageWithLabels
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.presentation.model.LabelUiModel
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelType
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList")
class ConversationDetailMessageUiModelMapper @Inject constructor(
    private val avatarUiModelMapper: DetailAvatarUiModelMapper,
    private val expirationTimeMapper: ExpirationTimeMapper,
    private val colorMapper: ColorMapper,
    private val formatShortTime: FormatShortTime,
    private val messageLocationUiModelMapper: MessageLocationUiModelMapper,
    private val resolveParticipantName: ResolveParticipantName,
    private val messageDetailHeaderUiModelMapper: MessageDetailHeaderUiModelMapper,
    private val messageBodyUiModelMapper: MessageBodyUiModelMapper
) {

    fun toUiModel(
        messageWithLabels: MessageWithLabels,
        contacts: List<Contact>,
        folderColorSettings: FolderColorSettings
    ): ConversationDetailMessageUiModel.Collapsed {
        val (message, labels) = messageWithLabels
        val senderResolvedName = resolveParticipantName(message.sender, contacts)
        return ConversationDetailMessageUiModel.Collapsed(
            avatar = avatarUiModelMapper(
                message,
                senderResolvedName = senderResolvedName
            ),
            expiration = message.expirationTimeOrNull()?.let(expirationTimeMapper::toUiModel),
            forwardedIcon = getForwardedIcon(isForwarded = message.isForwarded),
            hasAttachments = message.numAttachments > message.attachmentCount.calendar,
            isStarred = SystemLabelId.Starred.labelId in message.labelIds,
            isUnread = message.unread,
            locationIcon = messageLocationUiModelMapper(message.labelIds, labels, folderColorSettings),
            repliedIcon = getRepliedIcon(isReplied = message.isReplied, isRepliedAll = message.isRepliedAll),
            sender = senderResolvedName,
            shortTime = formatShortTime(message.time.seconds),
            labels = toLabelUiModels(messageWithLabels.labels),
            messageId = message.messageId
        )
    }

    fun toUiModel(
        messageWithLabels: MessageWithLabels,
        contacts: List<Contact>,
        decryptedMessageBody: DecryptedMessageBody,
        folderColorSettings: FolderColorSettings
    ): ConversationDetailMessageUiModel.Expanded {
        val (message, _) = messageWithLabels
        return ConversationDetailMessageUiModel.Expanded(
            messageId = message.messageId,
            isUnread = message.unread,
            messageDetailHeaderUiModel = messageDetailHeaderUiModelMapper.toUiModel(
                messageWithLabels,
                contacts,
                folderColorSettings
            ),
            messageBodyUiModel = messageBodyUiModelMapper.toUiModel(decryptedMessageBody)
        )
    }

    fun toUiModel(collapsed: ConversationDetailMessageUiModel.Collapsed): ConversationDetailMessageUiModel.Expanding {
        return ConversationDetailMessageUiModel.Expanding(
            collapsed = collapsed,
            messageId = collapsed.messageId
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

    private fun toLabelUiModels(labels: List<Label>): List<LabelUiModel> =
        labels.filter { it.type == LabelType.MessageLabel }.map { label ->
            LabelUiModel(
                name = label.name,
                color = colorMapper.toColor(label.color).getOrElse { Color.Unspecified }
            )
        }
}

