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
import ch.protonmail.android.maildetail.domain.repository.InMemoryConversationStateRepository
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.isReservedSystemLabelId
import ch.protonmail.android.maillabel.presentation.model.LabelUiModel
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.MessageWithLabels
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.presentation.model.isApplicable
import ch.protonmail.android.mailsettings.domain.model.AutoDeleteSetting
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.user.domain.entity.UserAddress
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList")
class ConversationDetailMessageUiModelMapper @Inject constructor(
    private val messageIdUiModelMapper: MessageIdUiModelMapper,
    private val avatarUiModelMapper: DetailAvatarUiModelMapper,
    private val expirationTimeMapper: ExpirationTimeMapper,
    private val colorMapper: ColorMapper,
    private val formatShortTime: FormatShortTime,
    private val messageLocationUiModelMapper: MessageLocationUiModelMapper,
    private val resolveParticipantName: ResolveParticipantName,
    private val messageDetailHeaderUiModelMapper: MessageDetailHeaderUiModelMapper,
    private val messageDetailFooterUiModelMapper: MessageDetailFooterUiModelMapper,
    private val messageBannersUiModelMapper: MessageBannersUiModelMapper,
    private val messageBodyUiModelMapper: MessageBodyUiModelMapper,
    private val participantUiModelMapper: ParticipantUiModelMapper
) {

    suspend fun toUiModel(
        messageWithLabels: MessageWithLabels,
        contacts: List<Contact>,
        folderColorSettings: FolderColorSettings,
        autoDeleteSetting: AutoDeleteSetting
    ): ConversationDetailMessageUiModel.Collapsed {
        val (message, labels) = messageWithLabels
        val senderResolvedName = resolveParticipantName(message.sender, contacts)
        return ConversationDetailMessageUiModel.Collapsed(
            avatar = avatarUiModelMapper(
                message,
                senderResolvedName = senderResolvedName.name
            ),
            expiration = message.expirationTimeOrNull()?.let(expirationTimeMapper::toUiModel),
            forwardedIcon = getForwardedIcon(isForwarded = message.isForwarded),
            hasAttachments = message.numAttachments > message.attachmentCount.calendar,
            isStarred = SystemLabelId.Starred.labelId in message.labelIds,
            isUnread = message.unread,
            locationIcon = messageLocationUiModelMapper(
                message.labelIds,
                labels,
                folderColorSettings,
                autoDeleteSetting
            ),
            repliedIcon = getRepliedIcon(isReplied = message.isReplied, isRepliedAll = message.isRepliedAll),
            sender = participantUiModelMapper.senderToUiModel(message.sender, contacts),
            shortTime = formatShortTime(message.time.seconds),
            labels = toLabelUiModels(messageWithLabels.labels),
            messageId = messageIdUiModelMapper.toUiModel(message.messageId),
            isDraft = message.isDraft()
        )
    }

    suspend fun toUiModel(
        messageWithLabels: MessageWithLabels,
        contacts: List<Contact>,
        decryptedMessageBody: DecryptedMessageBody,
        effect: InMemoryConversationStateRepository.PostExpandEffect?,
        folderColorSettings: FolderColorSettings,
        autoDeleteSetting: AutoDeleteSetting,
        userAddress: UserAddress,
        existingMessageUiState: ConversationDetailMessageUiModel.Expanded? = null
    ): ConversationDetailMessageUiModel.Expanded {
        val (message, _) = messageWithLabels
        val uiModel =
            messageBodyUiModelMapper.toUiModel(
                message.userId,
                decryptedMessageBody,
                existingMessageUiState?.messageBodyUiModel,
                effect = effect
            )
        return ConversationDetailMessageUiModel.Expanded(
            messageId = messageIdUiModelMapper.toUiModel(message.messageId),
            isUnread = message.unread,
            messageDetailHeaderUiModel = messageDetailHeaderUiModelMapper.toUiModel(
                messageWithLabels,
                contacts,
                folderColorSettings,
                autoDeleteSetting
            ),
            messageDetailFooterUiModel = messageDetailFooterUiModelMapper.toUiModel(messageWithLabels),
            messageBannersUiModel = messageBannersUiModelMapper.createMessageBannersUiModel(message),
            requestPhishingLinkConfirmation = message.isPhishing(),
            messageBodyUiModel = uiModel,
            expandCollapseMode = existingMessageUiState?.let {
                if (it.expandCollapseMode.isApplicable()) it.expandCollapseMode
                else getInitialBodyExpandCollapseMode(uiModel)
            } ?: getInitialBodyExpandCollapseMode(uiModel),
            userAddress = userAddress
        )
    }

    private fun getInitialBodyExpandCollapseMode(uiModel: MessageBodyUiModel): MessageBodyExpandCollapseMode {
        return if (uiModel.shouldShowExpandCollapseButton) {
            MessageBodyExpandCollapseMode.Collapsed
        } else {
            MessageBodyExpandCollapseMode.NotApplicable
        }
    }

    suspend fun toUiModel(
        message: ConversationDetailMessageUiModel.Expanded,
        messageWithLabels: MessageWithLabels,
        contacts: List<Contact>,
        folderColorSettings: FolderColorSettings,
        autoDeleteSetting: AutoDeleteSetting
    ): ConversationDetailMessageUiModel.Expanded {
        return message.copy(
            isUnread = messageWithLabels.message.unread,
            messageDetailHeaderUiModel = messageDetailHeaderUiModelMapper.toUiModel(
                messageWithLabels,
                contacts,
                folderColorSettings,
                autoDeleteSetting
            )
        )
    }

    fun toUiModel(collapsed: ConversationDetailMessageUiModel.Collapsed): ConversationDetailMessageUiModel.Expanding {
        return ConversationDetailMessageUiModel.Expanding(
            collapsed = collapsed,
            messageId = collapsed.messageId
        )
    }

    fun toUiModel(messageWithLabels: MessageWithLabels): ConversationDetailMessageUiModel.Hidden {
        return ConversationDetailMessageUiModel.Hidden(
            messageId = messageIdUiModelMapper.toUiModel(messageWithLabels.message.messageId),
            isUnread = messageWithLabels.message.unread
        )
    }

    private fun getForwardedIcon(isForwarded: Boolean): ConversationDetailMessageUiModel.ForwardedIcon = when {
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

    private fun toLabelUiModels(labels: List<Label>): ImmutableList<LabelUiModel> =
        labels.filter { it.type == LabelType.MessageLabel && !it.labelId.isReservedSystemLabelId() }.map { label ->
            LabelUiModel(
                name = label.name,
                color = colorMapper.toColor(label.color).getOrElse { Color.Unspecified },
                id = label.labelId.id
            )
        }.distinctBy { it.id }.toImmutableList()
}

