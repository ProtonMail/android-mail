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

import android.content.Context
import android.text.format.Formatter
import androidx.compose.ui.graphics.Color
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.usecase.FormatExtendedTime
import ch.protonmail.android.mailcommon.presentation.usecase.FormatShortTime
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageDetailHeaderUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.isReservedSystemLabelId
import ch.protonmail.android.maillabel.presentation.model.LabelUiModel
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithLabels
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import ch.protonmail.android.mailsettings.domain.model.AutoDeleteSetting
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelType
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class MessageDetailHeaderUiModelMapper @Inject constructor(
    private val colorMapper: ColorMapper,
    @ApplicationContext private val context: Context,
    private val detailAvatarUiModelMapper: DetailAvatarUiModelMapper,
    private val formatExtendedTime: FormatExtendedTime,
    private val formatShortTime: FormatShortTime,
    private val messageLocationUiModelMapper: MessageLocationUiModelMapper,
    private val participantUiModelMapper: ParticipantUiModelMapper,
    private val resolveParticipantName: ResolveParticipantName
) {

    suspend fun toUiModel(
        messageWithLabels: MessageWithLabels,
        contacts: List<Contact>,
        folderColorSettings: FolderColorSettings,
        autoDeleteSetting: AutoDeleteSetting
    ): MessageDetailHeaderUiModel {
        val senderResolvedName = resolveParticipantName(messageWithLabels.message.sender, contacts)
        return MessageDetailHeaderUiModel(
            avatar = detailAvatarUiModelMapper(messageWithLabels.message, senderResolvedName.name),
            sender = participantUiModelMapper.senderToUiModel(messageWithLabels.message.sender, contacts),
            shouldShowTrackerProtectionIcon = true,
            shouldShowAttachmentIcon = messageWithLabels.message.hasNonCalendarAttachments(),
            shouldShowStar = messageWithLabels.message.isStarred(),
            location = messageLocationUiModelMapper(
                messageWithLabels.message.labelIds,
                messageWithLabels.labels,
                folderColorSettings,
                autoDeleteSetting
            ),
            time = formatShortTime(messageWithLabels.message.time.seconds),
            extendedTime = formatExtendedTime(messageWithLabels.message.time.seconds),
            shouldShowUndisclosedRecipients = messageWithLabels.message.hasUndisclosedRecipients(),
            allRecipients = messageWithLabels.message.allRecipients(contacts),
            toRecipients = messageWithLabels.message.toList.map {
                participantUiModelMapper.recipientToUiModel(it, contacts)
            }.toImmutableList(),
            ccRecipients = messageWithLabels.message.ccList.map {
                participantUiModelMapper.recipientToUiModel(it, contacts)
            }.toImmutableList(),
            bccRecipients = messageWithLabels.message.bccList.map {
                participantUiModelMapper.recipientToUiModel(it, contacts)
            }.toImmutableList(),
            labels = toLabelUiModels(messageWithLabels.labels),
            size = Formatter.formatShortFileSize(context, messageWithLabels.message.size),
            encryptionPadlock = R.drawable.ic_proton_lock,
            encryptionInfo = "End-to-end encrypted and signed message",
            messageIdUiModel = toMessageUiModel(messageWithLabels.message.messageId)
        )
    }

    private fun Message.hasNonCalendarAttachments() = numAttachments > attachmentCount.calendar

    private fun Message.isStarred() = labelIds.any { it == MailLabelId.System.Starred.labelId }

    private fun Message.hasUndisclosedRecipients() = (toList + ccList + bccList).isEmpty()

    private fun Message.allRecipients(contacts: List<Contact>): TextUiModel {
        val allRecipientsList = toList + ccList + bccList

        return if (allRecipientsList.isNotEmpty()) {
            TextUiModel.Text(allRecipientsList.joinToString { resolveParticipantName(it, contacts).name })
        } else {
            TextUiModel.TextRes(R.string.undisclosed_recipients)
        }
    }

    private fun toLabelUiModels(labels: List<Label>): ImmutableList<LabelUiModel> =
        labels.filter { it.type == LabelType.MessageLabel && !it.labelId.isReservedSystemLabelId() }.map { label ->
            LabelUiModel(
                name = label.name,
                color = colorMapper.toColor(label.color).getOrElse { Color.Unspecified },
                id = label.labelId.id
            )
        }.distinctBy { it.id }.toImmutableList()

    private fun toMessageUiModel(messageId: MessageId) = MessageIdUiModel(messageId.id)
}
